package us.ihmc.darpaRoboticsChallenge.visualization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.CommonNames;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.inputdevices.SliderBoardConfigurationManager;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.humanoidRobot.partNames.NeckJointName;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

public class WalkControllerSliderBoard

{
   public WalkControllerSliderBoard(SimulationConstructionSet scs, YoVariableRegistry registry, DRCRobotModel drcRobotModel)
   {
      final EnumYoVariable<SliderBoardMode> sliderBoardMode = new EnumYoVariable<SliderBoardMode>("sliderBoardMode", registry, SliderBoardMode.class);
      final SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);

      sliderBoardConfigurationManager.setSlider(1, "captureKpParallel", registry, 0.0, 2.0);
      sliderBoardConfigurationManager.setKnob(1, "captureKpOrthogonal", registry, 0.0, 2.0);

      sliderBoardConfigurationManager.setSlider(2, "kp_comHeight", registry, 0.0, 40.0);
      sliderBoardConfigurationManager.setKnob(2, "kd_comHeight", registry, 0.0, 13.0);

      sliderBoardConfigurationManager.setSlider(3, "kpPelvisOrientation", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob(3, "zetaPelvisOrientation", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(4, "kpUpperBody", registry, 0.0, 200.0);
      sliderBoardConfigurationManager.setKnob(4, "zetaUpperBody", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(5, "kpAllArmJointsL", registry, 0.0, 120.0);
      sliderBoardConfigurationManager.setKnob(5, "zetaAllArmJointsL", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(6, "kpAllArmJointsR", registry, 0.0, 120.0);
      sliderBoardConfigurationManager.setKnob(6, "zetaAllArmJointsR", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(7, CommonNames.doIHMCControlRatio.toString(), registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(8, "offsetHeightAboveGround", registry, 0.0, 0.20);

      sliderBoardConfigurationManager.saveConfiguration(SliderBoardMode.WalkingGains.toString());
      
      sliderBoardConfigurationManager.clearControls();

      sliderBoardConfigurationManager.setButton(1, registry.getVariable("PelvisICPBasedTranslationManager","manualModeICPOffset"));
      sliderBoardConfigurationManager.setSlider(1, "desiredICPOffsetX", registry, -0.3, 0.3);
      sliderBoardConfigurationManager.setKnob(1, "desiredICPOffsetY", registry, -0.3, 0.3);
      
      sliderBoardConfigurationManager.setKnob(9, "desiredICPEccentricity", registry, 0, .9);
      sliderBoardConfigurationManager.setKnob(10, "desiredICPAngle", registry, -Math.PI, Math.PI);

//      sliderBoardConfigurationManager.setSlider(3, "hipXJointStiffness", registry, 3000, 20000.0);
//      sliderBoardConfigurationManager.setSlider(4, "hipZJointStiffness", registry, 3000, 20000.0);
//      sliderBoardConfigurationManager.setSlider(5, "hipYJointStiffness", registry, 3000, 20000.0);
//      sliderBoardConfigurationManager.setSlider(6, "otherJointsStiffness", registry, 3000, 20000.0);
      
      sliderBoardConfigurationManager.setKnob(2, "userSetDesiredPelvis", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(2, "userDesiredPelvisYaw", registry, -0.8, 0.8);
      sliderBoardConfigurationManager.setSlider(3, "userDesiredPelvisPitch", registry, -0.4, 0.4);
      sliderBoardConfigurationManager.setSlider(4, "userDesiredPelvisRoll", registry, -0.3, 0.3);

      sliderBoardConfigurationManager.setSlider(5, "userDesiredChestYaw", registry, -0.8, 0.8);
      sliderBoardConfigurationManager.setKnob(5, "userDesiredHeadYaw", registry, -0.8, 0.8);

      sliderBoardConfigurationManager.setSlider(6, "userDesiredChestPitch", registry, -0.8, 0.8);
      sliderBoardConfigurationManager.setKnob(6, "userDesiredHeadPitch", registry, -0.5, 0.5);
      
      sliderBoardConfigurationManager.setSlider(7, "userDesiredChestRoll", registry, -0.5, 0.5);
      sliderBoardConfigurationManager.setKnob(7, "userDesiredHeadRoll", registry, -0.8, 0.8);

      sliderBoardConfigurationManager.setSlider(8, "offsetHeightAboveGround", registry, 0.0, 0.20);

//    sliderBoardConfigurationManager.setKnob  (8, "sliderBoardMode", registry, 0.0, SliderBoardMode.values().length);
      sliderBoardConfigurationManager.setKnob(8, "gainScaleFactor", registry, 0.0, 1.0, 3.5, 0.0);
      
      sliderBoardConfigurationManager.saveConfiguration(SliderBoardMode.WalkingDesireds.toString());
      sliderBoardConfigurationManager.clearControls();
      
      /* Terrain Exploration Section */
      sliderBoardConfigurationManager.setSlider(1, "footCoPOffsetX", registry, -0.2, 0.2);
      sliderBoardConfigurationManager.setSlider(2, "footCoPOffsetY", registry, -0.1, 0.1);
      sliderBoardConfigurationManager.setSlider(3, "captureKpParallel", registry, 0.0, 2.0);
      sliderBoardConfigurationManager.setSlider(4, "captureKpOrthogonal", registry, 0.0, 2.0);
      sliderBoardConfigurationManager.setSlider(8, "offsetHeightAboveGround", registry, -0.20, 0.20);

      sliderBoardConfigurationManager.setButton(1, registry.getVariable("MomentumBasedController","FeetCoPControlIsActive"));
      
      sliderBoardConfigurationManager.saveConfiguration(SliderBoardMode.TerrainExploration.toString());
      sliderBoardConfigurationManager.clearControls();
      
      if(drcRobotModel.getWalkingControllerParameters().controlHeadAndHandsWithSliders())
      {
         setupHeadAndHandSliders(sliderBoardConfigurationManager, sliderBoardMode, drcRobotModel, registry);
      }

      //default
      sliderBoardMode.set(SliderBoardMode.WalkingGains);

      VariableChangedListener listener = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            System.out.println("SliderBoardMode: " + sliderBoardMode.getEnumValue().toString());
            sliderBoardConfigurationManager.loadConfiguration(sliderBoardMode.getEnumValue().toString());
         }
      };

      sliderBoardMode.addVariableChangedListener(listener);
      listener.variableChanged(null);

   }
   
   private void setupHeadAndHandSliders(final SliderBoardConfigurationManager sliderBoardConfigurationManager, final EnumYoVariable<SliderBoardMode> sliderBoardMode, 
         final DRCRobotModel drcRobotModel, final YoVariableRegistry registry)
   {
      
      final DoubleYoVariable leftGraspPercentage = new DoubleYoVariable("SliderLeftHandGraspPercentage", registry);
      final DoubleYoVariable rightGraspPercentage = new DoubleYoVariable("SliderRightHandGraspPercentage", registry);
      final DoubleYoVariable headYawPercentage = new DoubleYoVariable("SliderHeadYawPercentage", registry);
      final DoubleYoVariable lowerHeadPitchPercentage = new DoubleYoVariable("SliderLowerHeadPitchPercentage", registry);
      final DoubleYoVariable upperHeadPitchPercentage = new DoubleYoVariable("SliderUpperHeadPitchPercentage", registry);
      
      final SideDependentList<LinkedHashMap<String,Pair<Double,Double>>> sliderBoardControlledFingerJointsWithLimits = drcRobotModel.getSliderBoardControlledFingerJointsWithLimits();
      final LinkedHashMap<NeckJointName,Pair<Double,Double>> sliderBoardControlledNeckJointsWithLimits = drcRobotModel.getSliderBoardControlledNeckJointsWithLimits();
      
      sliderBoardConfigurationManager.setSlider(1, leftGraspPercentage, 0.0, 1.0);
      sliderBoardConfigurationManager.setSlider(2, rightGraspPercentage, 0.0, 1.0);
      
      if(Arrays.asList(drcRobotModel.getJointMap().getNeckJointNames()).contains(NeckJointName.NECK_YAW))
      {
         sliderBoardConfigurationManager.setKnob(1, headYawPercentage,0.0,1.0);
      }
      if(Arrays.asList(drcRobotModel.getJointMap().getNeckJointNames()).contains(NeckJointName.UPPER_NECK_PITCH))
      {
         sliderBoardConfigurationManager.setSlider(3, upperHeadPitchPercentage,0.0,1.0);
      }
      if(Arrays.asList(drcRobotModel.getJointMap().getNeckJointNames()).contains(NeckJointName.LOWER_NECK_PITCH))
      {
         sliderBoardConfigurationManager.setSlider(4, lowerHeadPitchPercentage,0.0,1.0);
      }
         
      sliderBoardConfigurationManager.saveConfiguration(SliderBoardMode.HeadAndGraspControl.toString());
      sliderBoardConfigurationManager.clearControls();
      
      if(Arrays.asList(drcRobotModel.getJointMap().getNeckJointNames()).contains(NeckJointName.NECK_YAW))
      {
         upperHeadPitchPercentage.addVariableChangedListener(new VariableChangedListener()
         {
            @Override
            public void variableChanged(YoVariable<?> v)
            {
               NeckJointName upperHeadPitch = NeckJointName.NECK_YAW;
               double jointRange = sliderBoardControlledNeckJointsWithLimits.get(upperHeadPitch).second() - 
                     sliderBoardControlledNeckJointsWithLimits.get(upperHeadPitch).first();
               DoubleYoVariable desiredAngle = (DoubleYoVariable) registry.getVariable(drcRobotModel.getJointMap().getNeckJointName(upperHeadPitch));
               desiredAngle.set(upperHeadPitchPercentage.getDoubleValue()*jointRange);
            }
         });
      }
      
      if(Arrays.asList(drcRobotModel.getJointMap().getNeckJointNames()).contains(NeckJointName.UPPER_NECK_PITCH))
      {
         upperHeadPitchPercentage.addVariableChangedListener(new VariableChangedListener()
         {
            @Override
            public void variableChanged(YoVariable<?> v)
            {
               NeckJointName upperHeadPitch = NeckJointName.UPPER_NECK_PITCH;
               double jointRange = sliderBoardControlledNeckJointsWithLimits.get(upperHeadPitch).second() - 
                     sliderBoardControlledNeckJointsWithLimits.get(upperHeadPitch).first();
               DoubleYoVariable desiredAngle = (DoubleYoVariable) registry.getVariable(drcRobotModel.getJointMap().getNeckJointName(upperHeadPitch));
               desiredAngle.set(upperHeadPitchPercentage.getDoubleValue()*jointRange);
            }
         });
      }
      
      if(Arrays.asList(drcRobotModel.getJointMap().getNeckJointNames()).contains(NeckJointName.LOWER_NECK_PITCH))
      {
         upperHeadPitchPercentage.addVariableChangedListener(new VariableChangedListener()
         {
            @Override
            public void variableChanged(YoVariable<?> v)
            {
               NeckJointName upperHeadPitch = NeckJointName.LOWER_NECK_PITCH;
               double jointRange = sliderBoardControlledNeckJointsWithLimits.get(upperHeadPitch).second() - 
                     sliderBoardControlledNeckJointsWithLimits.get(upperHeadPitch).first();
               DoubleYoVariable desiredAngle = (DoubleYoVariable) registry.getVariable(drcRobotModel.getJointMap().getNeckJointName(upperHeadPitch));
               desiredAngle.set(upperHeadPitchPercentage.getDoubleValue()*jointRange);
            }
         });
      }
      
      leftGraspPercentage.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            for(String actuatableFingerJointName : sliderBoardControlledFingerJointsWithLimits.get(RobotSide.LEFT).keySet())
            {
               double jointRange = sliderBoardControlledFingerJointsWithLimits.get(RobotSide.LEFT).get(actuatableFingerJointName).second()-
                     sliderBoardControlledFingerJointsWithLimits.get(RobotSide.LEFT).get(actuatableFingerJointName).first();
               DoubleYoVariable desiredAngle = (DoubleYoVariable) registry.getVariable(actuatableFingerJointName + CommonNames.q_d.toString());
               desiredAngle.set(leftGraspPercentage.getDoubleValue()*jointRange);
            }
         }
      });
      
      rightGraspPercentage.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            for(String actuatableFingerJointName : sliderBoardControlledFingerJointsWithLimits.get(RobotSide.RIGHT).keySet())
            {
               double jointRange = sliderBoardControlledFingerJointsWithLimits.get(RobotSide.RIGHT).get(actuatableFingerJointName).second()-
                     sliderBoardControlledFingerJointsWithLimits.get(RobotSide.RIGHT).get(actuatableFingerJointName).first();
               DoubleYoVariable desiredAngle = (DoubleYoVariable) registry.getVariable(actuatableFingerJointName + CommonNames.q_d.toString());
               desiredAngle.set(rightGraspPercentage.getDoubleValue()*jointRange);
            }
         }
      });
   }

   private enum SliderBoardMode {WalkingGains, WalkingDesireds, TerrainExploration, HeadAndGraspControl};
}
