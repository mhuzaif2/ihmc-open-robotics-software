package us.ihmc.quadrupedRobotics.planning.chooser.footstepChooser;

import us.ihmc.euclid.referenceFrame.FrameOrientation2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.sensorProcessing.frames.CommonQuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.geometry.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.quadrupedRobotics.mechanics.inverseKinematics.QuadrupedLinkLengths;
import us.ihmc.commons.MathTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.GeometryTools;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.referenceFrames.MidFrameZUpFrame;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.TranslationReferenceFrame;
import us.ihmc.robotics.robotSide.RobotEnd;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.time.ExecutionTimer;

public class MidFootZUpSwingTargetGenerator implements SwingTargetGenerator
{
   private final double MINIMUM_VELOCITY_FOR_FULL_SKEW = 0.1;
   public static double MINIMUM_DISTANCE_FROM_SAMESIDE_FOOT = 0.04;
   public static double DEFAULT_STRIDE_LENGTH = 1.373;//0.34;
   public static double DEFAULT_STANCE_WIDTH = 0.36922 * 2;//0.24;
   public static double DEFAULT_MAX_SKEW = 0.1;
   public static double DEFAULT_MAX_YAW = 0.25;

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final CommonQuadrupedReferenceFrames referenceFrames;
   private final YoDouble minimumVelocityForFullSkew = new YoDouble("minimumVelocityForFullSkew", registry);
   private final YoDouble strideLength = new YoDouble("strideLength", registry);
   private final YoDouble stanceWidth = new YoDouble("stanceWidth", registry);
   private final YoDouble maxForwardSkew = new YoDouble("maxForwardSkew", registry);
   private final YoDouble maxLateralSkew = new YoDouble("maxLateralSkew", registry);
   private final YoDouble maxYawPerStep = new YoDouble("maxYawPerStep", registry);
   private final YoDouble minimumDistanceFromSameSideFoot = new YoDouble("minimumDistanceFromSameSideFoot", registry);
   private final YoDouble xOffsetFromCenterOfHips = new YoDouble("xOffsetFromCenterOfHips", registry);
   private final YoDouble yOffsetFromCenterOfHips = new YoDouble("yOffsetFromCenterOfHips", registry);
   private final QuadrupedSupportPolygon supportPolygon = new QuadrupedSupportPolygon();
   private final FramePoint3D centroid = new FramePoint3D(ReferenceFrame.getWorldFrame());
   
   private final FramePoint3D tempFootPositionSameSideOppositeEnd = new FramePoint3D();
   private final FramePoint3D tempFootPositionOppositeSideSameEnd = new FramePoint3D();
   
   private final FrameOrientation2D tempOppositeSideOrientation = new FrameOrientation2D();
   
   private final ExecutionTimer getSwingTargetTimer = new ExecutionTimer("getSwingTargetTimer", registry);

   private final FramePoint3D swingLegHipPitchPoint = new FramePoint3D();
   private final FrameQuaternion swingLegHipRollOrientation = new FrameQuaternion();

   private final FramePoint3D desiredSwingFootPositionFromHalfStride = new FramePoint3D();
   private final FramePoint3D desiredSwingFootPositionFromOppositeSideFoot = new FramePoint3D();

   private final FrameVector3D footDesiredVector= new FrameVector3D();
   private final Vector2D footDesiredVector2d= new Vector2D();
   
   private final QuadrupedLinkLengths linkLengths;

   public MidFootZUpSwingTargetGenerator(SwingTargetGeneratorParameters footStepParameters, CommonQuadrupedReferenceFrames referenceFrames,
         YoVariableRegistry parentRegistry)
   {
      this.referenceFrames = referenceFrames;
      this.linkLengths = new QuadrupedLinkLengths(referenceFrames);
      parentRegistry.addChild(registry);
      
      if (footStepParameters != null)
      {
         minimumDistanceFromSameSideFoot.set(footStepParameters.getMinimumDistanceFromSameSideFoot());
         minimumVelocityForFullSkew.set(footStepParameters.getMinimumVelocityForFullSkew());
         strideLength.set(footStepParameters.getStanceLength());
         stanceWidth.set(footStepParameters.getStanceWidth());
         maxForwardSkew.set(footStepParameters.getMaxForwardSkew());
         maxLateralSkew.set(footStepParameters.getMaxLateralSkew());
         maxYawPerStep.set(footStepParameters.getMaxYawPerStep());
      }
      else
      {
         minimumDistanceFromSameSideFoot.set(MINIMUM_DISTANCE_FROM_SAMESIDE_FOOT);
         minimumVelocityForFullSkew.set(MINIMUM_VELOCITY_FOR_FULL_SKEW);
         strideLength.set(DEFAULT_STRIDE_LENGTH);
         stanceWidth.set(DEFAULT_STANCE_WIDTH);
         maxForwardSkew.set(DEFAULT_MAX_SKEW);
         maxYawPerStep.set(DEFAULT_MAX_YAW);
      }
   }

   @Override
   public void getSwingTarget(RobotQuadrant swingLeg, FrameVector3DReadOnly desiredBodyVelocity, FramePoint3D swingTargetToPack, double desiredYawRate)
   {
      updateFeetPositions();
      RobotSide oppositeSide = swingLeg.getOppositeSide();
      ReferenceFrame oppositeSideZUpFrame = referenceFrames.getSideDependentMidFeetZUpFrame(oppositeSide);
      calculateSwingTarget(supportPolygon, oppositeSideZUpFrame, swingLeg, desiredBodyVelocity, desiredYawRate, swingTargetToPack);
   }

   private TranslationReferenceFrame hindFoot = new TranslationReferenceFrame("backFoot", ReferenceFrame.getWorldFrame());
   private TranslationReferenceFrame frontFoot = new TranslationReferenceFrame("frontFoot", ReferenceFrame.getWorldFrame());
   private PoseReferenceFrame projectedFrameBeforeLegPitch = new PoseReferenceFrame("projectedFrameBeforeLegPitch", ReferenceFrame.getWorldFrame());
   private MidFrameZUpFrame midFeetZUpFrame = new MidFrameZUpFrame("MidFeetZUpFrame", ReferenceFrame.getWorldFrame(), hindFoot, frontFoot);
   
   private final FramePose3D projectedLegPitchPose = new FramePose3D();
   private final FramePoint3D projectedLegPitchPosition = new FramePoint3D();
   private final FrameVector3D scaledVelocityVector = new FrameVector3D();
   
   @Override
   public void getSwingTarget(RobotQuadrant swingLeg, ReferenceFrame swingLegAttachmentFrame, FrameVector3DReadOnly desiredBodyVelocity, double swingDuration, FramePoint3D swingTargetToPack,
         double desiredYawRate)
   {
      getSwingTargetTimer.startMeasurement();
      
      getSwingTarget(swingLeg, desiredBodyVelocity, swingTargetToPack, desiredYawRate);
      
      ReferenceFrame frameBeforeHipPitch = referenceFrames.getFrameBeforeLegJoint(swingLeg, LegJointName.HIP_PITCH);
      projectedLegPitchPosition.setToZero(frameBeforeHipPitch);
      projectedLegPitchPose.setToZero(frameBeforeHipPitch);
      
      scaledVelocityVector.setIncludingFrame(desiredBodyVelocity);
      scaledVelocityVector.scale(swingDuration);
      
      scaledVelocityVector.changeFrame(frameBeforeHipPitch);
      projectedLegPitchPosition.add(scaledVelocityVector);
      projectedLegPitchPose.setPosition(projectedLegPitchPosition);
      
//      double amountToYaw = desiredYawRate * swingDuration;
//      projectedLegPitchPose.rotatePoseAboutAxis(hipPitchFrame, Axis.Z, amountToYaw);
      
      projectedLegPitchPose.changeFrame(ReferenceFrame.getWorldFrame());
      projectedFrameBeforeLegPitch.setPoseAndUpdate(projectedLegPitchPose);
      
      swingTargetToPack.changeFrame(frameBeforeHipPitch);
      footDesiredVector.setIncludingFrame(swingTargetToPack);
      
      double requestedFootOffset = footDesiredVector.length();
      double maxFootOffset = getMaxSwingDistanceGivenBodyVelocity(swingLeg, swingTargetToPack, desiredBodyVelocity, swingDuration);
      
      if(requestedFootOffset > maxFootOffset)
      {
         footDesiredVector2d.setX(footDesiredVector.getX());
         footDesiredVector2d.setY(footDesiredVector.getY());
         double zHeight = swingTargetToPack.getZ();
         double maxReach =  Math.sqrt(maxFootOffset * maxFootOffset - zHeight * zHeight); 
         double scalar = maxReach / footDesiredVector2d.length() * 0.96;
         footDesiredVector2d.scale(scalar);
         swingTargetToPack.setX(footDesiredVector2d.getX());
         swingTargetToPack.setY(footDesiredVector2d.getY());
      }      
      swingTargetToPack.changeFrame(ReferenceFrame.getWorldFrame());
      
      getSwingTargetTimer.stopMeasurement();
   }
   
   private double getMaxSwingDistanceGivenBodyVelocity(RobotQuadrant swingLeg, FramePoint3DReadOnly swingTarget, FrameVector3DReadOnly desiredBodyVelocity, double swingDuration)
   {
      double legLength = linkLengths.getThighLength(swingLeg) + linkLengths.getShinLength(swingLeg);
      double zHeight = swingTarget.getZ();
      double hipPitch = Math.acos(zHeight / legLength);
      double theta =  Math.PI / 2.0 + hipPitch;
      
      scaledVelocityVector.setIncludingFrame(desiredBodyVelocity);
      scaledVelocityVector.scale(swingDuration);
      
      double bodyShiftLength = scaledVelocityVector.length();
      
      //law of cosines used to get distance from current hip Pitch to foot at singularity given projected hip position and current z height 
      double sidesSquared = bodyShiftLength * bodyShiftLength + legLength * legLength;
      double maxSwingDistance = sidesSquared - 2 * legLength * bodyShiftLength * Math.cos(theta);
      maxSwingDistance = Math.sqrt(maxSwingDistance);
      
      return maxSwingDistance;
   }
   
   @Override
   public void getSwingTarget(QuadrupedSupportPolygon footPostions, RobotQuadrant swingLeg, FrameVector3DReadOnly desiredBodyVelocity, FramePoint3D swingTargetToPack,
         double desiredYawRate)
   {
      FramePoint3D across = footPostions.getFootstep(swingLeg.getAcrossBodyQuadrant());
      FramePoint3D diag = footPostions.getFootstep(swingLeg.getDiagonalOppositeQuadrant());

      if(swingLeg.isQuadrantInFront())
      {
         frontFoot.updateTranslation(across);
         hindFoot.updateTranslation(diag);
      }
      else
      {
         frontFoot.updateTranslation(diag);
         hindFoot.updateTranslation(across);
      }
      midFeetZUpFrame.update();
      
      calculateSwingTarget(footPostions, midFeetZUpFrame, swingLeg, desiredBodyVelocity, desiredYawRate, swingTargetToPack);
   }

   private void calculateSwingTarget(QuadrupedSupportPolygon supportPolygon, ReferenceFrame oppositeSideZUpFrame, RobotQuadrant swingLeg, FrameVector3DReadOnly desiredBodyVelocity, double desiredYawRate,
         FramePoint3D swingTargetToPack)
   {
      //calculate hipPitchHeight
      swingLegHipPitchPoint.setToZero(referenceFrames.getHipPitchFrame(swingLeg));
      swingLegHipPitchPoint.changeFrame(ReferenceFrame.getWorldFrame());
      double swingLegHipPitchHeight = swingLegHipPitchPoint.getZ();

      //calculate hip Roll
      swingLegHipRollOrientation.setToZero(referenceFrames.getLegAttachmentFrame(swingLeg));
      swingLegHipRollOrientation.changeFrame(ReferenceFrame.getWorldFrame());
      double stepDistanceRemovedBecauseOfRoll = referenceFrames.getLegLength(swingLeg) * Math.sin(Math.abs(swingLegHipRollOrientation.getRoll()));

      double maxStepDistance;
      double maxStepDistanceWithNoRoll = Math.sqrt(Math.pow(referenceFrames.getLegLength(swingLeg), 2) - Math.pow(swingLegHipPitchHeight, 2));
      if (Double.isNaN(maxStepDistanceWithNoRoll) || Double.isNaN(stepDistanceRemovedBecauseOfRoll))
      {
         maxStepDistance = 0.0;
      }
      else
      {
         maxStepDistance = maxStepDistanceWithNoRoll - stepDistanceRemovedBecauseOfRoll;
         maxStepDistance = Math.max(maxStepDistance, 0.0);
      }

      double deltaYaw = MathTools.clamp(desiredYawRate, maxYawPerStep.getDoubleValue());

      RobotQuadrant sameSideQuadrant = swingLeg.getSameSideQuadrant();
      RobotQuadrant sameEndQuadrant = swingLeg.getAcrossBodyQuadrant();

      tempFootPositionSameSideOppositeEnd.setIncludingFrame(supportPolygon.getFootstep(sameSideQuadrant));
      tempFootPositionOppositeSideSameEnd.setIncludingFrame(supportPolygon.getFootstep(sameEndQuadrant));

      //midZUpFrame is oriented so X is perpendicular to the two same side feet, Y pointing backward
      determineFootPositionFromHalfStride(supportPolygon, swingLeg, desiredBodyVelocity, maxStepDistance, deltaYaw, tempFootPositionSameSideOppositeEnd,
            tempFootPositionSameSideOppositeEnd, oppositeSideZUpFrame);

      determineFootPositionFromOppositeSideFoot(supportPolygon, swingLeg, desiredBodyVelocity, maxStepDistance, deltaYaw, tempFootPositionSameSideOppositeEnd,
            tempFootPositionOppositeSideSameEnd, oppositeSideZUpFrame);
      
      //pack the destination with 20% of the position from halfStride and 80% of the position from the opposite side foot
      desiredSwingFootPositionFromHalfStride.scale(0.2);
      desiredSwingFootPositionFromOppositeSideFoot.scale(0.8);
      
      swingTargetToPack.set(desiredSwingFootPositionFromHalfStride);
      swingTargetToPack.add(desiredSwingFootPositionFromOppositeSideFoot);
      swingTargetToPack.add(xOffsetFromCenterOfHips.getDoubleValue(), yOffsetFromCenterOfHips.getDoubleValue(), 0.0);
      
      //for debug
      //      swingTargetToPack.set(desiredSwingFootPositionFromHalfStride);
      //      swingTargetToPack.set(desiredSwingFootPositionFromOppositeSideFoot);
   }

   private void determineFootPositionFromHalfStride(QuadrupedSupportPolygon supportPolygon, RobotQuadrant swingLeg, FrameVector3DReadOnly desiredBodyVelocity, double maxStepDistance, double deltaYaw,
          FramePoint3D footPositionSameSideOppositeEnd, FramePoint3DReadOnly footPositionOppositeSideSameEnd, ReferenceFrame oppositeSideZUpFrame)
   {
      RobotSide swingSide = swingLeg.getSide();
      RobotEnd robotEnd = swingLeg.getEnd();

      //check the difference in orientation between the oppositeSideZUpFrame and the bodyZUpFrame
      double orientationDeltaWithBody = calculateOppositeSideOrientationWithRespectToBody(oppositeSideZUpFrame);
      
      //handle forward backward placement
      desiredSwingFootPositionFromHalfStride.setToZero(oppositeSideZUpFrame);
      double halfStrideLength = 0.5 * strideLength.getDoubleValue();
      double clippedSkew = MathTools.clamp(maxForwardSkew.getDoubleValue(), 0.0, halfStrideLength);
      double clippedSkewScalar = MathTools.clamp(desiredBodyVelocity.getX() / minimumVelocityForFullSkew.getDoubleValue(), 1.0);
      double amountToSkew = clippedSkewScalar * clippedSkew;
      amountToSkew = MathTools.clamp(amountToSkew, maxStepDistance);
      double newY = robotEnd.negateIfFrontEnd(halfStrideLength) - amountToSkew;
      desiredSwingFootPositionFromHalfStride.setY(newY);

      //handle left right placement
      double halfStanceWidth = 0.5 * stanceWidth.getDoubleValue();
      double lateralClippedSkew = MathTools.clamp(maxLateralSkew.getDoubleValue(), 0.0, halfStanceWidth);
      double lateralClippedSkewScalar = MathTools.clamp(desiredBodyVelocity.getY() / minimumVelocityForFullSkew.getDoubleValue(), 1.0);
      double lateralAmountToSkew = lateralClippedSkewScalar * lateralClippedSkew;
      lateralAmountToSkew = MathTools.clamp(lateralAmountToSkew, maxStepDistance);
      double newX = swingSide.negateIfRightSide(stanceWidth.getDoubleValue()) + lateralAmountToSkew;
      desiredSwingFootPositionFromHalfStride.setX(newX);
      
      // maintain minimumDistanceFromSameSideFoot inline
      footPositionSameSideOppositeEnd.changeFrame(oppositeSideZUpFrame);
      double minimumRadiusFromSameSideFoot = minimumDistanceFromSameSideFoot.getDoubleValue();

      boolean footIsForwardOfOtherFoot = desiredSwingFootPositionFromHalfStride.getY() < footPositionSameSideOppositeEnd.getY();
      boolean footIsBehindOtherFoot = desiredSwingFootPositionFromHalfStride.getY() > footPositionSameSideOppositeEnd.getY();
      boolean footIsCloseToOtherFoot = desiredSwingFootPositionFromHalfStride.distance(footPositionSameSideOppositeEnd) < minimumRadiusFromSameSideFoot;

      if ((robotEnd.equals(RobotEnd.HIND) && footIsForwardOfOtherFoot) || (robotEnd.equals(RobotEnd.FRONT) && footIsBehindOtherFoot) || footIsCloseToOtherFoot)
      {
         desiredSwingFootPositionFromHalfStride.setY(footPositionSameSideOppositeEnd.getY());
         desiredSwingFootPositionFromHalfStride.add(0.0, robotEnd.negateIfFrontEnd(minimumRadiusFromSameSideFoot), 0.0);
      }

      //compensate the angular diplacement if the footsepts of the side used as reference is not aligned with the body
      GeometryTools.yawAboutPoint(desiredSwingFootPositionFromHalfStride, footPositionOppositeSideSameEnd, -orientationDeltaWithBody, desiredSwingFootPositionFromHalfStride);
      
      desiredSwingFootPositionFromHalfStride.changeFrame(ReferenceFrame.getWorldFrame());

      //rotate the foot about the centroid of the predicted foot polygon
      supportPolygon.setFootstep(swingLeg, desiredSwingFootPositionFromHalfStride);
      supportPolygon.getCentroid(centroid);
      GeometryTools.yawAboutPoint(desiredSwingFootPositionFromHalfStride, centroid, deltaYaw, desiredSwingFootPositionFromHalfStride);
   }

   private void determineFootPositionFromOppositeSideFoot(QuadrupedSupportPolygon supportPolygon, RobotQuadrant swingLeg, FrameVector3DReadOnly desiredBodyVelocity, double maxStepDistance, double deltaYaw,
         FramePoint3D footPositionSameSideOppositeEnd, FramePoint3D footPositionOppositeSideSameEnd, ReferenceFrame oppositeSideZUpFrame)
   {
      RobotSide swingSide = swingLeg.getSide();
      
      //check the difference in orientation between the oppositeSideZUpFrame and the bodyZUpFrame
      double orientationDeltaWithBody = calculateOppositeSideOrientationWithRespectToBody(oppositeSideZUpFrame);
      
      desiredSwingFootPositionFromOppositeSideFoot.setToZero(oppositeSideZUpFrame);
      double halfStrideLength = 0.5 * strideLength.getDoubleValue();
      double clippedSkew = MathTools.clamp(maxForwardSkew.getDoubleValue(), 0.0, halfStrideLength);
      double clippedSkewScalar = MathTools.clamp(desiredBodyVelocity.getX() / minimumVelocityForFullSkew.getDoubleValue(), 1.0);
      double amountToSkew = clippedSkewScalar * clippedSkew;
      amountToSkew = MathTools.clamp(amountToSkew, maxStepDistance);
      
      footPositionOppositeSideSameEnd.changeFrame(oppositeSideZUpFrame);      
      
      double newY = footPositionOppositeSideSameEnd.getY() - amountToSkew;

      // maintain minimumDistanceFromSameSideFoot inline
      footPositionSameSideOppositeEnd.changeFrame(oppositeSideZUpFrame);
      if(swingLeg.isQuadrantInHind() && (footPositionSameSideOppositeEnd.getY() + minimumDistanceFromSameSideFoot.getDoubleValue() > newY ))
         newY = footPositionSameSideOppositeEnd.getY() + minimumDistanceFromSameSideFoot.getDoubleValue();
      
      desiredSwingFootPositionFromOppositeSideFoot.setY(newY);
      
      //handle left right placement
      double halfStanceWidth = 0.5 * stanceWidth.getDoubleValue();
      double lateralClippedSkew = MathTools.clamp(maxLateralSkew.getDoubleValue(), 0.0, halfStanceWidth);
      double lateralClippedSkewScalar = MathTools.clamp(desiredBodyVelocity.getY() / minimumVelocityForFullSkew.getDoubleValue(), 1.0);
      double lateralAmountToSkew = lateralClippedSkewScalar * lateralClippedSkew;
      lateralAmountToSkew = MathTools.clamp(lateralAmountToSkew, maxStepDistance);
      
      double newX = swingSide.negateIfRightSide(stanceWidth.getDoubleValue()) + lateralAmountToSkew;
      desiredSwingFootPositionFromOppositeSideFoot.setX(newX);
      
      //compensate the angular diplacement if the footsepts of the side used as reference is not aligned with the body
      GeometryTools.yawAboutPoint(desiredSwingFootPositionFromOppositeSideFoot, footPositionOppositeSideSameEnd, -orientationDeltaWithBody, desiredSwingFootPositionFromOppositeSideFoot);
      
      desiredSwingFootPositionFromOppositeSideFoot.changeFrame(ReferenceFrame.getWorldFrame());
      
      //rotate the foot about the centroid of the predicted foot polygon
      supportPolygon.setFootstep(swingLeg, desiredSwingFootPositionFromOppositeSideFoot);
      supportPolygon.getCentroid(centroid);
      GeometryTools.yawAboutPoint(desiredSwingFootPositionFromOppositeSideFoot, centroid, deltaYaw, desiredSwingFootPositionFromOppositeSideFoot);
   }

   private double calculateOppositeSideOrientationWithRespectToBody(ReferenceFrame oppositeSideZUpFrame)
   {
      tempOppositeSideOrientation.setIncludingFrame(oppositeSideZUpFrame, 0.0);
      tempOppositeSideOrientation.changeFrame(referenceFrames.getBodyZUpFrame());
      return tempOppositeSideOrientation.getYaw() - Math.PI / 2.0;
   }

   private void updateFeetPositions()
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         FramePoint3D footPosition = supportPolygon.getFootstep(robotQuadrant);
         if(footPosition == null)
         {
            footPosition = new FramePoint3D(ReferenceFrame.getWorldFrame());
         }
         
         footPosition.setToZero(referenceFrames.getFootFrame(robotQuadrant));
         footPosition.changeFrame(ReferenceFrame.getWorldFrame());
         supportPolygon.setFootstep(robotQuadrant, footPosition);
      }
   }
}
