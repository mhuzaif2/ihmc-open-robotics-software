package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.Optional;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.graphics3DDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.algorithms.SphereWithConvexPolygonIntersector;
import us.ihmc.robotics.geometry.shapes.FrameSphere3d;
import us.ihmc.robotics.geometry.shapes.Plane3d;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.math.YoCounter;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.referenceFrames.TransformReferenceFrame;
import us.ihmc.robotics.trajectories.TrajectoryType;

public class SwingOverPlanarRegionsTrajectoryExpander
{
   private static final ReferenceFrame WORLD = ReferenceFrame.getWorldFrame();
   private static final double ignoreDistanceFromFloor = 0.02;

   private final TwoWaypointSwingGenerator twoWaypointSwingGenerator;

   private final IntegerYoVariable numberOfCheckpoints;
   private final YoCounter numberOfTriesCounter;
   private final DoubleYoVariable minimumClearance;
   private final DoubleYoVariable incrementalAdjustmentDistance;
   private final DoubleYoVariable maximumAdjustmentDistance;
   private final EnumYoVariable<SwingOverPlanarRegionsTrajectoryExpansionStatus> status;

   private final FrameConvexPolygon2d frameFootPolygon;
   private final YoFramePoint trajectoryPosition;
   private final PoseReferenceFrame solePoseReferenceFrame;
   private final RecyclingArrayList<FramePoint> originalWaypoints;
   private final RecyclingArrayList<FramePoint> adjustedWaypoints;
   private final double minimumSwingHeight;
   private final double maximumSwingHeight;
   private final double soleToToeLength;

   private final SphereWithConvexPolygonIntersector sphereWithConvexPolygonIntersector;
   private final FramePoint closestPolygonPoint;
   private boolean isIntersecting;
   private boolean isIntersectingAndAbovePlane;
   private final FrameSphere3d footCollisionSphere;
   private final FrameConvexPolygon2d framePlanarRegion;
   private final TransformReferenceFrame planarRegionReferenceFrame;
   private final Vector3d waypointAdjustmentVector;
   private final Plane3d waypointAdjustmentPlane;
   private final Plane3d swingFloorPlane;
   private final AxisAngle4d axisAngle;
   private final RigidBodyTransform rigidBodyTransform;

   // Boilerplate variables
   private final FrameVector initialVelocity;
   private final FrameVector touchdownVelocity;
   private final FramePoint toeOffPosition;
   private final FramePoint swingEndPosition;
   private final FramePoint stanceFootPosition;

   // Anti-garbage variables
   private final RigidBodyTransform planarRegionTransform;

   // Visualization
   private Optional<Updatable> visualizer;

   public enum SwingOverPlanarRegionsTrajectoryExpansionStatus
   {
      INITIALIZED, FAILURE_HIT_MAX_ADJUSTMENT_DISTANCE, SEARCHING_FOR_SOLUTION, SOLUTION_FOUND,
   }

   public SwingOverPlanarRegionsTrajectoryExpander(WalkingControllerParameters walkingControllerParameters, YoVariableRegistry parentRegistry,
                                                   YoGraphicsListRegistry graphicsListRegistry)
   {
      String namePrefix = "trajectoryExpander";
      twoWaypointSwingGenerator = new TwoWaypointSwingGenerator(namePrefix, walkingControllerParameters.getMinSwingHeightFromStanceFoot(),
                                                                walkingControllerParameters.getMaxSwingHeightFromStanceFoot(), parentRegistry,
                                                                graphicsListRegistry);
      minimumSwingHeight = walkingControllerParameters.getMinSwingHeightFromStanceFoot();
      maximumSwingHeight = walkingControllerParameters.getMaxSwingHeightFromStanceFoot();
      soleToToeLength = walkingControllerParameters.getFootLength() / 2.0;
      System.out.println("soltotoelength: " + soleToToeLength);

      numberOfCheckpoints = new IntegerYoVariable(namePrefix + "NumberOfCheckpoints", parentRegistry);
      numberOfTriesCounter = new YoCounter(namePrefix + "NumberOfTriesCounter", parentRegistry);
      minimumClearance = new DoubleYoVariable(namePrefix + "MinimumClearance", parentRegistry);
      incrementalAdjustmentDistance = new DoubleYoVariable(namePrefix + "IncrementalAdjustmentDistance", parentRegistry);
      maximumAdjustmentDistance = new DoubleYoVariable(namePrefix + "MaximumAdjustmentDistance", parentRegistry);
      status = new EnumYoVariable<SwingOverPlanarRegionsTrajectoryExpansionStatus>(namePrefix + "Status", parentRegistry,
                                                                                   SwingOverPlanarRegionsTrajectoryExpansionStatus.class);

      frameFootPolygon = new FrameConvexPolygon2d();
      trajectoryPosition = new YoFramePoint(namePrefix + "TrajectoryPosition", WORLD, parentRegistry);
      solePoseReferenceFrame = new PoseReferenceFrame(namePrefix + "SolePoseReferenceFrame", WORLD);
      originalWaypoints = new RecyclingArrayList<>(2, FramePoint.class);
      adjustedWaypoints = new RecyclingArrayList<>(2, FramePoint.class);

      sphereWithConvexPolygonIntersector = new SphereWithConvexPolygonIntersector();
      closestPolygonPoint = new FramePoint();
      footCollisionSphere = new FrameSphere3d();
      framePlanarRegion = new FrameConvexPolygon2d();
      planarRegionReferenceFrame = new TransformReferenceFrame("planarRegionReferenceFrame", WORLD);
      waypointAdjustmentVector = new Vector3d();
      waypointAdjustmentPlane = new Plane3d();
      swingFloorPlane = new Plane3d();
      axisAngle = new AxisAngle4d();
      rigidBodyTransform = new RigidBodyTransform();

      initialVelocity = new FrameVector(WORLD, 0.0, 0.0, 0.0);
      touchdownVelocity = new FrameVector(WORLD, 0.0, 0.0, walkingControllerParameters.getDesiredTouchdownVelocity());
      toeOffPosition = new FramePoint();
      swingEndPosition = new FramePoint();
      stanceFootPosition = new FramePoint();

      planarRegionTransform = new RigidBodyTransform();

      visualizer = Optional.empty();

      // Set default values
      numberOfCheckpoints.set(100);
      numberOfTriesCounter.setMaxCount(50);
      minimumClearance.set(0.04);
      incrementalAdjustmentDistance.set(0.03);
      maximumAdjustmentDistance.set(maximumSwingHeight - minimumSwingHeight);
   }

   public void expandTrajectoryOverPlanarRegions(ConvexPolygon2d footPolygonSoleFrame, FramePose stanceFootPose, FramePose swingStartPose,
                                                 FramePose swingEndPose, PlanarRegionsList planarRegionsList)
   {
      stanceFootPose.getPositionIncludingFrame(stanceFootPosition);
      stanceFootPosition.changeFrame(WORLD);
      twoWaypointSwingGenerator.setStanceFootPosition(stanceFootPosition);

      swingStartPose.getPositionIncludingFrame(toeOffPosition);
      toeOffPosition.changeFrame(WORLD);
      twoWaypointSwingGenerator.setInitialConditions(toeOffPosition, initialVelocity);

      swingEndPose.getPositionIncludingFrame(swingEndPosition);
      swingEndPosition.changeFrame(WORLD);
      twoWaypointSwingGenerator.setFinalConditions(swingEndPosition, touchdownVelocity);
      twoWaypointSwingGenerator.setStepTime(1.0);

      double[] defaultWaypointProportions = TwoWaypointSwingGenerator.getDefaultWaypointProportions();
      originalWaypoints.get(0).setToZero();
      originalWaypoints.get(0).interpolate(toeOffPosition, swingEndPosition, defaultWaypointProportions[0]);
      originalWaypoints.get(0).add(0.0, 0.0, minimumSwingHeight);
      adjustedWaypoints.get(0).set(originalWaypoints.get(0));
      originalWaypoints.get(1).setToZero();
      originalWaypoints.get(1).interpolate(toeOffPosition, swingEndPosition, defaultWaypointProportions[1]);
      originalWaypoints.get(1).add(0.0, 0.0, minimumSwingHeight);
      adjustedWaypoints.get(1).set(originalWaypoints.get(1));

      status.set(SwingOverPlanarRegionsTrajectoryExpansionStatus.SEARCHING_FOR_SOLUTION);
      numberOfTriesCounter.resetCount();
      while (status.getEnumValue().equals(SwingOverPlanarRegionsTrajectoryExpansionStatus.SEARCHING_FOR_SOLUTION) && !numberOfTriesCounter.maxCountReached())
      {
         status.set(tryATrajectory(footPolygonSoleFrame, planarRegionsList));
         updateVisualizer();
         numberOfTriesCounter.countOne();
      }
   }

   private SwingOverPlanarRegionsTrajectoryExpansionStatus tryATrajectory(ConvexPolygon2d footPolygonSoleFrame, PlanarRegionsList planarRegionsList)
   {
      twoWaypointSwingGenerator.setTrajectoryType(TrajectoryType.CUSTOM, adjustedWaypoints);
      twoWaypointSwingGenerator.initialize();

      double stepAmount = 1.0 / (double) numberOfCheckpoints.getIntegerValue();
      for (double time = 0.0; time < 1.0; time += stepAmount)
      {
         twoWaypointSwingGenerator.compute(time);
         FramePoint frameTupleUnsafe = trajectoryPosition.getFrameTuple();
         twoWaypointSwingGenerator.getPosition(frameTupleUnsafe);
         trajectoryPosition.setWithoutChecks(frameTupleUnsafe);
         solePoseReferenceFrame.setPositionAndUpdate(trajectoryPosition.getFrameTuple());

         footCollisionSphere.setToZero(WORLD);
         footCollisionSphere.setRadius(soleToToeLength);
         footCollisionSphere.getSphere3d().setPosition(solePoseReferenceFrame.getPositionUnsafe());
         frameFootPolygon.setIncludingFrame(solePoseReferenceFrame, footPolygonSoleFrame);

         footCollisionSphere.changeFrame(WORLD);

         Point3d center = new Point3d();
         footCollisionSphere.getCenter(center);
         frameFootPolygon.changeFrameAndProjectToXYPlane(WORLD);

         waypointAdjustmentPlane.setPoints(toeOffPosition.getPoint(), adjustedWaypoints.get(0).getPoint(), swingEndPosition.getPoint());

         axisAngle.set(waypointAdjustmentPlane.getNormal(), Math.PI / 2.0);
         rigidBodyTransform.setRotation(axisAngle);
         swingFloorPlane.setPoint(toeOffPosition.getPoint());
         swingFloorPlane.getNormal().sub(toeOffPosition.getPoint(), swingEndPosition.getPoint());
         rigidBodyTransform.transform(swingFloorPlane.getNormal());
         swingFloorPlane.getNormal().normalize();

         closestPolygonPoint.setIncludingFrame(WORLD, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
         isIntersecting = false;
         isIntersectingAndAbovePlane = false;

         for (int i = 0; i < planarRegionsList.getNumberOfPlanarRegions(); i++)
         {
            PlanarRegion planarRegion = planarRegionsList.getPlanarRegion(i);
            planarRegion.getTransformToWorld(planarRegionTransform);
            planarRegionReferenceFrame.setTransformAndUpdate(planarRegionTransform);
            for (int j = 0; j < planarRegion.getNumberOfConvexPolygons(); j++)
            {
               framePlanarRegion.setIncludingFrame(planarRegionReferenceFrame, planarRegion.getConvexPolygon(j));

               boolean intersectionExists = sphereWithConvexPolygonIntersector.checkIfIntersectionExists(footCollisionSphere, framePlanarRegion);

               if (footCollisionSphere.distance(sphereWithConvexPolygonIntersector.getClosestPointOnPolygon()) < footCollisionSphere.distance(closestPolygonPoint))
               {
                  closestPolygonPoint.set(sphereWithConvexPolygonIntersector.getClosestPointOnPolygon());
               }

               if (intersectionExists)
               {
                  isIntersecting = true;

                  if (swingFloorPlane.isOnOrAbove(sphereWithConvexPolygonIntersector.getClosestPointOnPolygon().getPoint())
                        && swingFloorPlane.distance(sphereWithConvexPolygonIntersector.getClosestPointOnPolygon().getPoint()) > ignoreDistanceFromFloor)
                  {
                     isIntersectingAndAbovePlane = true;

                     axisAngle.set(waypointAdjustmentPlane.getNormal(), Math.PI * time);
                     rigidBodyTransform.setRotation(axisAngle);

                     waypointAdjustmentVector.sub(toeOffPosition.getPoint(), swingEndPosition.getPoint());
                     waypointAdjustmentVector.normalize();
                     rigidBodyTransform.transform(waypointAdjustmentVector);
                     waypointAdjustmentVector.scale(incrementalAdjustmentDistance.getDoubleValue());
                     waypointAdjustmentVector.scale(1.0 - time);
                     adjustedWaypoints.get(0).add(waypointAdjustmentVector);

                     waypointAdjustmentVector.sub(toeOffPosition.getPoint(), swingEndPosition.getPoint());
                     waypointAdjustmentVector.normalize();
                     rigidBodyTransform.transform(waypointAdjustmentVector);
                     waypointAdjustmentVector.scale(incrementalAdjustmentDistance.getDoubleValue());
                     waypointAdjustmentVector.scale(time);
                     adjustedWaypoints.get(1).add(waypointAdjustmentVector);

                     if (adjustedWaypoints.get(0).distance(originalWaypoints.get(0)) > maximumAdjustmentDistance.getDoubleValue()
                           || adjustedWaypoints.get(1).distance(originalWaypoints.get(1)) > maximumAdjustmentDistance.getDoubleValue())
                     {
                        return SwingOverPlanarRegionsTrajectoryExpansionStatus.FAILURE_HIT_MAX_ADJUSTMENT_DISTANCE;
                     }

                     return SwingOverPlanarRegionsTrajectoryExpansionStatus.SEARCHING_FOR_SOLUTION;
                  }
               }
            }
         }

         updateVisualizer();
      }

      return SwingOverPlanarRegionsTrajectoryExpansionStatus.SOLUTION_FOUND;
   }

   public RecyclingArrayList<FramePoint> getExpandedWaypoints()
   {
      return adjustedWaypoints;
   }

   public SwingOverPlanarRegionsTrajectoryExpansionStatus getStatus()
   {
      return status.getEnumValue();
   }

   // VISULIZER METHODS

   public void updateVisualizer()
   {
      if (visualizer.isPresent())
      {
         visualizer.get().update(0.0);
      }
   }

   public void attachVisualizer(Updatable visualizer)
   {
      this.visualizer = Optional.of(visualizer);
   }

   public PoseReferenceFrame getSolePoseReferenceFrame()
   {
      return solePoseReferenceFrame;
   }

   public FramePoint getClosestPolygonPoint()
   {
      return closestPolygonPoint;
   }

   public boolean isIntersecting()
   {
      return isIntersecting;
   }

   public boolean isIntersectingAndAbovePlane()
   {
      return isIntersectingAndAbovePlane;
   }

   public double getSphereRadius()
   {
      return footCollisionSphere.getRadius();
   }
}
