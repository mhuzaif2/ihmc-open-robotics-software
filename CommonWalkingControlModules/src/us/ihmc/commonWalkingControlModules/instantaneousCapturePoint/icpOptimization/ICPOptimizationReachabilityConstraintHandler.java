package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;

public class ICPOptimizationReachabilityConstraintHandler
{
   private final DoubleYoVariable lateralReachabilityOuterLimit;
   private final DoubleYoVariable lateralReachabilityInnerLimit;
   private final DoubleYoVariable forwardReachabilityLimit;
   private final DoubleYoVariable backwardReachabilityLimit;

   private final BipedSupportPolygons bipedSupportPolygons;

   private final FramePoint2d tempPoint2d = new FramePoint2d();

   public ICPOptimizationReachabilityConstraintHandler(BipedSupportPolygons bipedSupportPolygons, ICPOptimizationParameters icpOptimizationParameters,
         String yoNamePrefix, YoVariableRegistry registry)
   {
      this.bipedSupportPolygons = bipedSupportPolygons;

      lateralReachabilityOuterLimit = new DoubleYoVariable(yoNamePrefix + "LateralReachabilityOuterLimit", registry);
      lateralReachabilityInnerLimit = new DoubleYoVariable(yoNamePrefix + "LateralReachabilityInnerLimit", registry);

      forwardReachabilityLimit = new DoubleYoVariable(yoNamePrefix + "ForwardReachabilityLimit", registry);
      backwardReachabilityLimit = new DoubleYoVariable(yoNamePrefix + "BackwardReachabilityLimit", registry);

      lateralReachabilityInnerLimit.set(icpOptimizationParameters.getLateralReachabilityInnerLimit());
      lateralReachabilityOuterLimit.set(Math.max(lateralReachabilityInnerLimit.getDoubleValue(), icpOptimizationParameters.getLateralReachabilityOuterLimit()));

      forwardReachabilityLimit.set(icpOptimizationParameters.getForwardReachabilityLimit());
      backwardReachabilityLimit.set(icpOptimizationParameters.getBackwardReachabilityLimit());
   }

   public void updateReachabilityConstraintForSingleSupport(RobotSide supportSide, ICPOptimizationSolver solver)
   {
      solver.resetReachabilityConstraint();

      double lateralInnerLimit = supportSide.negateIfLeftSide(lateralReachabilityInnerLimit.getDoubleValue());
      double lateralOuterLimit = supportSide.negateIfLeftSide(lateralReachabilityOuterLimit.getDoubleValue());

      ReferenceFrame supportSoleFrame = bipedSupportPolygons.getSoleZUpFrames().get(supportSide);

      tempPoint2d.setToZero(supportSoleFrame);

      tempPoint2d.set(forwardReachabilityLimit.getDoubleValue(), lateralInnerLimit);
      solver.addReachabilityVertex(tempPoint2d, supportSoleFrame);

      tempPoint2d.set(forwardReachabilityLimit.getDoubleValue(), lateralOuterLimit);
      solver.addReachabilityVertex(tempPoint2d, supportSoleFrame);

      tempPoint2d.set(backwardReachabilityLimit.getDoubleValue(), lateralInnerLimit);
      solver.addReachabilityVertex(tempPoint2d, supportSoleFrame);

      tempPoint2d.set(backwardReachabilityLimit.getDoubleValue(), lateralOuterLimit);
      solver.addReachabilityVertex(tempPoint2d, supportSoleFrame);
   }

   public void updateReachabilityConstraintForDoubleSupport(ICPOptimizationSolver solver)
   {
      solver.resetReachabilityConstraint();
   }
}
