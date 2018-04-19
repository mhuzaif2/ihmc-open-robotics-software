package us.ihmc.quadrupedRobotics.planning;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.quadrupedRobotics.planning.stepStream.QuadrupedPlanarFootstepPlan;
import us.ihmc.quadrupedRobotics.util.PreallocatedList;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.*;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;

public class QuadrupedXGaitPlanner
{
   private final FramePoint3D goalPosition;
   private final FramePoint3D goalPositionAdjustment;
   private final QuadrantDependentList<FramePoint3D> xGaitRectangleVertices;
   private final FramePose3D xGaitRectanglePose;
   private final FramePose3D xGaitRectanglePoseAtSoS;
   private final PoseReferenceFrame xGaitRectangleFrame;
   private final EndDependentList<QuadrupedTimedStep> pastSteps;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   public QuadrupedXGaitPlanner()
   {
      goalPosition = new FramePoint3D();
      goalPositionAdjustment = new FramePoint3D();
      xGaitRectangleVertices = new QuadrantDependentList<>();
      xGaitRectanglePose = new FramePose3D(worldFrame);
      xGaitRectanglePoseAtSoS = new FramePose3D(worldFrame);
      xGaitRectangleFrame = new PoseReferenceFrame("xGaitRectangleFrame", worldFrame);
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         xGaitRectangleVertices.set(robotQuadrant, new FramePoint3D(xGaitRectangleFrame));
      }
      pastSteps = new EndDependentList<>();
      pastSteps.put(RobotEnd.FRONT, new QuadrupedTimedStep());
      pastSteps.put(RobotEnd.HIND, new QuadrupedTimedStep());
   }

   public void computeInitialPlan(QuadrupedPlanarFootstepPlan footstepPlan, Vector3D planarVelocity, RobotQuadrant initialStepQuadrant,
                                  FramePoint3D supportCentroidAtSoS, double timeAtSoS, double yawAtSoS, QuadrupedXGaitSettingsReadOnly xGaitSettings)
   {
      // initialize nominal support rectangle
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         xGaitRectangleVertices.get(robotQuadrant).changeFrame(xGaitRectangleFrame);
         xGaitRectangleVertices.get(robotQuadrant).setX(robotQuadrant.getEnd().negateIfHindEnd(xGaitSettings.getStanceLength() / 2.0));
         xGaitRectangleVertices.get(robotQuadrant).setY(robotQuadrant.getSide().negateIfRightSide(xGaitSettings.getStanceWidth() / 2.0));
         xGaitRectangleVertices.get(robotQuadrant).setZ(0);
      }
      ReferenceFrame supportCentroidFrame = supportCentroidAtSoS.getReferenceFrame();
      supportCentroidAtSoS.changeFrame(worldFrame);
      xGaitRectanglePoseAtSoS.setPosition(supportCentroidAtSoS);
      xGaitRectanglePoseAtSoS.setOrientationYawPitchRoll(yawAtSoS, 0, 0);
      xGaitRectanglePose.set(xGaitRectanglePoseAtSoS);
      supportCentroidAtSoS.changeFrame(supportCentroidFrame);

      // plan steps
      double lastStepStartTime = timeAtSoS;
      RobotQuadrant lastStepQuadrant = initialStepQuadrant.getNextReversedRegularGaitSwingQuadrant();
      PreallocatedList<QuadrupedTimedOrientedStep> plannedSteps = footstepPlan.getPlannedSteps();
      plannedSteps.clear();
      for (int i = 0; i < plannedSteps.capacity(); i++)
      {
         plannedSteps.add();
         QuadrupedTimedOrientedStep step = plannedSteps.get(plannedSteps.size() - 1);

         // compute step quadrant
         RobotQuadrant thisStepQuadrant = lastStepQuadrant.getNextRegularGaitSwingQuadrant();
         step.setRobotQuadrant(thisStepQuadrant);

         // compute step timing
         double thisStepStartTime;
         double thisStepEndTime;
         if (i == 0)
         {
            thisStepStartTime = timeAtSoS;
            thisStepEndTime = timeAtSoS + xGaitSettings.getStepDuration();
         }
         else
         {
            double endPhaseShift = thisStepQuadrant.isQuadrantInHind() ? 180.0 - xGaitSettings.getEndPhaseShift() : xGaitSettings.getEndPhaseShift();
            double endTimeShift = xGaitSettings.getEndDoubleSupportDuration() + xGaitSettings.getStepDuration();
            endTimeShift *= Math.max(Math.min(endPhaseShift, 180.0), 0.0) / 180.0;
            thisStepStartTime = lastStepStartTime + endTimeShift;
            thisStepEndTime = thisStepStartTime + xGaitSettings.getStepDuration();
         }
         step.getTimeInterval().setStartTime(thisStepStartTime);
         step.getTimeInterval().setEndTime(thisStepEndTime);

         // compute xGait rectangle pose at end of step
         double deltaTime = thisStepEndTime - timeAtSoS;
         extrapolatePose(xGaitRectanglePose, planarVelocity, deltaTime);
         xGaitRectangleFrame.setPoseAndUpdate(xGaitRectanglePose);
         step.setStepYaw(xGaitRectanglePose.getYaw());

         // compute step goal position by sampling the corner position of the xGait rectangle at touch down
         RobotQuadrant robotQuadrant = step.getRobotQuadrant();
         goalPosition.setIncludingFrame(xGaitRectangleVertices.get(robotQuadrant));
         step.setGoalPosition(goalPosition);

         // compute step ground clearance
         step.setGroundClearance(xGaitSettings.getStepGroundClearance());

         // update state for next step
         lastStepStartTime = thisStepStartTime;
         lastStepQuadrant = thisStepQuadrant;
      }
   }

   public void computeOnlinePlan(QuadrupedPlanarFootstepPlan footstepPlan,
         Vector3D planarVelocity, double currentTime, double currentYaw, double currentHeight, QuadrupedXGaitSettingsReadOnly xGaitSettings)
   {
      // initialize latest step
      QuadrupedTimedStep latestStep;
      EndDependentList<QuadrupedTimedOrientedStep> currentSteps = footstepPlan.getCurrentSteps();

      if (currentSteps.get(RobotEnd.HIND).getTimeInterval().getEndTime() > currentSteps.get(RobotEnd.FRONT).getTimeInterval().getEndTime())
         latestStep = currentSteps.get(RobotEnd.HIND);
      else
         latestStep = currentSteps.get(RobotEnd.FRONT);

      // initialize nominal support rectangle
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         xGaitRectangleVertices.get(robotQuadrant).changeFrame(xGaitRectangleFrame);
         xGaitRectangleVertices.get(robotQuadrant).setX(robotQuadrant.getEnd().negateIfHindEnd(xGaitSettings.getStanceLength() / 2.0));
         xGaitRectangleVertices.get(robotQuadrant).setY(robotQuadrant.getSide().negateIfRightSide(xGaitSettings.getStanceWidth() / 2.0));
         xGaitRectangleVertices.get(robotQuadrant).setZ(0);
      }
      xGaitRectanglePoseAtSoS.setPosition(0, 0, currentHeight);
      xGaitRectanglePoseAtSoS.setOrientationYawPitchRoll(currentYaw, 0, 0);
      xGaitRectanglePose.set(xGaitRectanglePoseAtSoS);

      PreallocatedList<QuadrupedTimedOrientedStep> plannedSteps = footstepPlan.getPlannedSteps();
      plannedSteps.clear();
      // compute step quadrants and time intervals
      {
         RobotEnd thisStepEnd = latestStep.getRobotQuadrant().getOppositeEnd();
         pastSteps.set(RobotEnd.FRONT, currentSteps.get(RobotEnd.FRONT));
         pastSteps.set(RobotEnd.HIND, currentSteps.get(RobotEnd.HIND));

         for (int i = 0; i < plannedSteps.capacity(); i++)
         {
            plannedSteps.add();
            QuadrupedTimedStep thisStep = plannedSteps.get(i);
            QuadrupedTimedStep pastStepOnSameEnd = pastSteps.get(thisStepEnd);
            QuadrupedTimedStep pastStepOnOppositeEnd = pastSteps.get(thisStepEnd.getOppositeEnd());

            thisStep.setRobotQuadrant(pastStepOnSameEnd.getRobotQuadrant().getAcrossBodyQuadrant());
            computeStepTimeInterval(thisStep, pastStepOnSameEnd, pastStepOnOppositeEnd, xGaitSettings);
            if (currentTime > thisStep.getTimeInterval().getStartTime())
               thisStep.getTimeInterval().shiftInterval(currentTime - thisStep.getTimeInterval().getStartTime());

            pastSteps.set(thisStepEnd, thisStep);
            thisStepEnd = thisStepEnd.getOppositeEnd();
         }
      }

      // compute step goal positions and ground clearances
      {
         for (int i = 0; i < plannedSteps.size(); i++)
         {
            // compute xGait rectangle pose at end of step
            double deltaTime = plannedSteps.get(i).getTimeInterval().getEndTime() - currentTime;
            extrapolatePose(xGaitRectanglePose, planarVelocity, deltaTime);
            xGaitRectangleFrame.setPoseAndUpdate(xGaitRectanglePose);
            plannedSteps.get(i).setStepYaw(xGaitRectanglePose.getYaw());

            // compute step goal position by sampling the corner position of the xGait rectangle at touchdown
            RobotQuadrant stepQuadrant = plannedSteps.get(i).getRobotQuadrant();
            goalPosition.setIncludingFrame(xGaitRectangleVertices.get(stepQuadrant));
            plannedSteps.get(i).setGoalPosition(goalPosition);

            // compute step ground clearance
            plannedSteps.get(i).setGroundClearance(xGaitSettings.getStepGroundClearance());
         }
      }

      // translate step goal positions based on latest step position
      {
         // compute xGait rectangle pose at end of step
         double deltaTime = latestStep.getTimeInterval().getEndTime() - currentTime;
         extrapolatePose(xGaitRectanglePose, planarVelocity, deltaTime);
         xGaitRectangleFrame.setPoseAndUpdate(xGaitRectanglePose);

         // compute step goal position
         RobotQuadrant stepQuadrant = latestStep.getRobotQuadrant();
//         goalPositionAdjustment.setIncludingFrame(xGaitRectangleVertices.get(stepQuadrant));
//         goalPositionAdjustment.changeFrame(worldFrame);

         // compute step goal adjustment
         FramePoint3D nominalGoalPosition = xGaitRectangleVertices.get(stepQuadrant);
         nominalGoalPosition.changeFrame(worldFrame);
//         latestStep.getGoalPosition(goalPositionAdjustment);
//         goalPositionAdjustment.changeFrame(worldFrame);
//         goalPositionAdjustment.sub(nominalGoalPosition);

         // compensate for position error
//         for (int i = 0; i < plannedSteps.size(); i++)
//         {
//            plannedSteps.get(i).getGoalPosition(goalPosition);
//            goalPosition.changeFrame(worldFrame);
//            goalPosition.addX(goalPositionAdjustment.getX());
//            goalPosition.addY(goalPositionAdjustment.getY());
//            plannedSteps.get(i).setGoalPosition(goalPosition);
//         }
      }
   }

   private final Vector3D translation = new Vector3D();

   private void extrapolatePose(FramePose3D poseToExtrapolateToPack, Vector3DReadOnly planarVelocity, double deltaTime)
   {
      double currentYaw = poseToExtrapolateToPack.getYaw();

      // initialize forward, lateral, and rotational velocity in pose frame
      double forwardVelocity = planarVelocity.getX();
      double lateralVelocity = planarVelocity.getY();
      double rotationalVelocity = planarVelocity.getZ();

      // compute extrapolated pose assuming a constant planar velocity
      double yawRotation;
      double epsilon = 0.001;
      if (MathTools.epsilonEquals(rotationalVelocity, 0.0, epsilon))
      {
         yawRotation = 0.0;
         translation.setX((forwardVelocity * Math.cos(yawRotation) - lateralVelocity * Math.sin(yawRotation)) * deltaTime);
         translation.setY((forwardVelocity * Math.sin(yawRotation) + lateralVelocity * Math.cos(yawRotation)) * deltaTime);
      }
      else
      {
         yawRotation = rotationalVelocity * deltaTime;
         translation.setX(forwardVelocity / rotationalVelocity * (Math.sin(yawRotation) - Math.sin(currentYaw)) + lateralVelocity / rotationalVelocity * (Math.cos(yawRotation) - Math.cos(currentYaw)));
         translation.setY(-forwardVelocity / rotationalVelocity * (Math.cos(yawRotation) - Math.cos(currentYaw)) + lateralVelocity / rotationalVelocity * (Math.sin(yawRotation) - Math.sin(currentYaw)));
      }

      poseToExtrapolateToPack.appendTranslation(translation);
      poseToExtrapolateToPack.appendYawRotation(yawRotation);
   }

   private void computeStepTimeInterval(QuadrupedTimedStep thisStep, QuadrupedTimedStep pastStepOnSameEnd, QuadrupedTimedStep pastStepOnOppositeEnd,
         QuadrupedXGaitSettingsReadOnly xGaitSettings)
   {
      RobotEnd thisStepEnd = thisStep.getRobotQuadrant().getEnd();
      RobotSide thisStepSide = thisStep.getRobotQuadrant().getSide();
      RobotSide pastStepSide = pastStepOnOppositeEnd.getRobotQuadrant().getSide();

      double pastStepEndTimeForSameEnd = pastStepOnSameEnd.getTimeInterval().getEndTime();
      double pastStepEndTimeForOppositeEnd = pastStepOnOppositeEnd.getTimeInterval().getEndTime();

      // Compute support durations and end phase shift.
      double nominalStepDuration = xGaitSettings.getStepDuration();
      double endDoubleSupportDuration = xGaitSettings.getEndDoubleSupportDuration();
      double endPhaseShift = MathTools.clamp(xGaitSettings.getEndPhaseShift(), 0, 359);
      if (thisStepEnd == RobotEnd.HIND)
         endPhaseShift = 360 - endPhaseShift;
      if (pastStepSide != thisStepSide)
         endPhaseShift = endPhaseShift - 180;

      // Compute step time interval. Step duration is scaled in the range (1.0, 1.5) to account for end phase shifts.
      double thisStepStartTime = pastStepEndTimeForSameEnd + endDoubleSupportDuration;
      double thisStepEndTime = pastStepEndTimeForOppositeEnd + (nominalStepDuration + endDoubleSupportDuration) * endPhaseShift / 180.0;
      double thisStepDuration = MathTools.clamp(thisStepEndTime - thisStepStartTime, nominalStepDuration, 1.5 * nominalStepDuration);

      thisStep.getTimeInterval().setStartTime(thisStepStartTime);
      thisStep.getTimeInterval().setEndTime(thisStepStartTime + thisStepDuration);
   }
}