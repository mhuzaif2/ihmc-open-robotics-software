package us.ihmc.sensorProcessing.encoder.comparison;

import java.util.ArrayList;
import java.util.HashMap;

import us.ihmc.sensorProcessing.encoder.SimulatedEncoder;
import us.ihmc.sensorProcessing.encoder.processors.EncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.JerryEncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.NaiveEncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.NonlinearObserverEncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.PolynomialFittingEncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.StateMachineEncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.StateMachineSimpleEncoderProcessor;
import us.ihmc.sensorProcessing.encoder.processors.StateMachineTwoEncoderProcessor;

import com.yobotics.simulationconstructionset.DataBuffer;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.IntYoVariable;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class EncoderProcessorComparer
{
   private final YoVariableRegistry registry;
   private final HashMap<EncoderProcessor, String> encoderProcessors = new HashMap<EncoderProcessor, String>();
   private final HashMap<EncoderProcessor, DoubleYoVariable> processedPositions = new HashMap<EncoderProcessor, DoubleYoVariable>();
   private final HashMap<EncoderProcessor, DoubleYoVariable> processedRates = new HashMap<EncoderProcessor, DoubleYoVariable>();
   private final IntYoVariable rawTicks;
   private final DoubleYoVariable rawPosition;
   private final DoubleYoVariable time;
   private final DoubleYoVariable actualPosition;
   private final DoubleYoVariable actualRate, actualRateInTicksPerSecond;
   private final DataBuffer dataBuffer;

   private final ArrayList<EncoderProcessorEvaluationTrajectory> jointTrajectories;
   private final double maxTime;
   private final double dt;
   private final SimulatedEncoder encoder;
   private final Thread simThread;

   public EncoderProcessorComparer(ArrayList<EncoderProcessorEvaluationTrajectory> jointTrajectories, double maxTime, double dt,
                                   double encoderTicksPerUnitOfPosition)
   {
      Robot nullRobot = new Robot("nullRobot");
      registry = nullRobot.getRobotsYoVariableRegistry();
      rawTicks = new IntYoVariable("rawTicks", registry);
      rawPosition = new DoubleYoVariable("rawPosition", registry);
      time = nullRobot.getYoTime();
      actualPosition = new DoubleYoVariable("actualPosition", registry);
      actualRate = new DoubleYoVariable("actualRate", registry);
      actualRateInTicksPerSecond = new DoubleYoVariable("actualRateInTicksPerSecond", registry);

      this.jointTrajectories = jointTrajectories;
      this.maxTime = maxTime;
      this.dt = dt;
      this.encoder = new SimulatedEncoder(encoderTicksPerUnitOfPosition, "comparerEncoder", registry);

      encoderProcessors.put(new StateMachineEncoderProcessor("StateMachine", rawTicks, dt, registry), "StateMachine");
      encoderProcessors.put(new StateMachineSimpleEncoderProcessor("StateMachineSimple", rawTicks, time, registry), "StateMachineSimple");
      encoderProcessors.put(new StateMachineTwoEncoderProcessor("StateMachineTwo", rawTicks, time, registry), "StateMachineTwo");
      encoderProcessors.put(new NonlinearObserverEncoderProcessor("NonlinObserver", rawPosition, dt, registry), "NonlinObserver");
      encoderProcessors.put(new NaiveEncoderProcessor("Naive", rawTicks, time, registry), "Naive");
      encoderProcessors.put(new PolynomialFittingEncoderProcessor("PolyFit320", rawTicks, time, 3, 2, 0, true, registry), "PolyFit320");
      encoderProcessors.put(new PolynomialFittingEncoderProcessor("PolyFit532", rawTicks, time, 5, 3, 2, true, registry), "PolyFit532");
      encoderProcessors.put(new PolynomialFittingEncoderProcessor("PolyFit820", rawTicks, time, 8, 2, 0, true, registry), "PolyFit820");
      encoderProcessors.put(new PolynomialFittingEncoderProcessor("PolyFit820nolimit", rawTicks, time, 8, 2, 0, false, registry), "PolyFit820nolimit");
      encoderProcessors.put(new JerryEncoderProcessor("Jerry", rawTicks, time, dt, registry), "Jerry");


      for (EncoderProcessor encoderProcessor : encoderProcessors.keySet())
      {
         encoderProcessor.setUnitDistancePerCount(1.0 / encoderTicksPerUnitOfPosition);
         processedPositions.put(encoderProcessor, new DoubleYoVariable("p_" + encoderProcessors.get(encoderProcessor), registry));
         processedRates.put(encoderProcessor, new DoubleYoVariable("pd_" + encoderProcessors.get(encoderProcessor), registry));
      }

      SimulationConstructionSet scs = new SimulationConstructionSet(nullRobot, (int) Math.ceil(maxTime * jointTrajectories.size() / dt) + 1);
      scs.hideViewport();
      dataBuffer = scs.getDataBuffer();
      dataBuffer.setWrapBuffer(false);
      simThread = new Thread(scs, "R2Simulation sim thread");
   }

   public void start()
   {
      simThread.start();

      double timeStart = 0.0;
      time.set(0.0);

      double lastActualPosition = 0.0;

      for (EncoderProcessorEvaluationTrajectory jointTrajectory : jointTrajectories)
      {
//       time.set(0.0);
         timeStart = time.getDoubleValue();
         jointTrajectory.update(time.getDoubleValue());
         double deltaPosition = lastActualPosition - jointTrajectory.getPosition();

         while (time.getDoubleValue() < maxTime + timeStart)
         {
            double actualPosition = deltaPosition + jointTrajectory.getPosition();
            this.actualPosition.set(actualPosition);
            encoder.setActualPosition(actualPosition);
            rawTicks.set(encoder.getEncoderTicks());
            rawPosition.set(encoder.getPositionFromEncoder());

            double actualVelocity = jointTrajectory.getVelocity();
            this.actualRate.set(actualVelocity);
            this.actualRateInTicksPerSecond.set(actualVelocity * encoder.getEncoderTicksPerUnitOfPosition());

            for (EncoderProcessor processor : encoderProcessors.keySet())
            {
               processor.update();
               processedPositions.get(processor).set(processor.getQ());
               processedRates.get(processor).set(processor.getQd());
            }

            dataBuffer.tickAndUpdate();
            
            int numJointUpdatesPerEncoderEvent = 20;
            for (int k=0; k<numJointUpdatesPerEncoderEvent; k++)
            {
               time.add(dt/((double) numJointUpdatesPerEncoderEvent));
               jointTrajectory.update(time.getDoubleValue());
            }
         }

         lastActualPosition = this.actualPosition.getDoubleValue();
      }
   }

   public static void main(String[] args)
   {
      double maxTime = 2.0;
      double dt = 2.4e-3; //7.2e-3; //2.0e-3;    // 7.2e-3;    // 7.2e-3;    // 1e-5;

      // Do everything in meters.
      double encoderCountsPerMeter = 2000.0 / 2.54 * 100.0;    // 100.0;    // 78740.1575;    // 100.0 // (2000 counts / inch) * (1.0 inches / 2.54 cm) * (100.0 cm / 1.0 m)

      System.out.println("encoderCountsPerMeter = " + encoderCountsPerMeter);
      System.out.println("meters per count = " + 1.0 / encoderCountsPerMeter);

      ArrayList<EncoderProcessorEvaluationTrajectory> jointTrajectories = new ArrayList<EncoderProcessorEvaluationTrajectory>();

      EncoderProcessorEvaluationTrajectory sinusoidalTrajectory = new SinusoidalEncoderProcessorEvaluationTrajectory(0.5 * 2.0 * Math.PI, 0.005, Math.PI);

      EncoderProcessorEvaluationTrajectory chirpTrajectory = new ChirpEncoderProcessorEvaluationTrajectory(5.0, 0.00005);

      EncoderProcessorEvaluationTrajectory dipDownTrajectory = new DipDownButDontReverseEncoderProcessorEvaluationTrajectory(6.0, 0.02, 0.0);

      EncoderProcessorEvaluationTrajectory sawtoothTrajectory = new SawtoothEncoderProcessorEvaluationTrajectory(0.267, 0.2);
      
      EncoderProcessorEvaluationTrajectory bangBangTrajectory = new BangBangEncoderProcessorEvaluationTrajectory(200.0, 0.03, 0.025);
      EncoderProcessorEvaluationTrajectory bangBangTrajectory2 = new BangBangEncoderProcessorEvaluationTrajectory(50.0, 0.06, 0.03);

      EncoderProcessorEvaluationTrajectory multipleSinusoidTrajectory = new MultipleSinusoidEncoderProcessorEvaluationTrajectory(
            new double[] {0.25 * 2.0 * Math.PI, 1.3 * 2.0 * Math.PI, 6.0 * 2.0 * Math.PI, 12.0 * 2.0 * Math.PI, 100.0 * 2.0 * Math.PI}, 
            new double[] {0.1, 0.02, 0.005, 0.0001, 0.0001},
            new double[] {0.0, 0.5, 0.7, 1.1, 1.4});


      EncoderProcessorEvaluationTrajectory multipleSinusoidTrajectory2 = new MultipleSinusoidEncoderProcessorEvaluationTrajectory(new double[] {0.25 * 2.0 * Math.PI,
              1.3 * 2.0 * Math.PI}, new double[] {0.1, 0.02}, new double[] {0.0, 0.5});

      EncoderProcessorEvaluationTrajectory decayingTrajectory = new DecayingEncoderProcessorEvaluationTrajectory(1.0, 0.2, 0.0, 1.0);

      EncoderProcessorEvaluationTrajectory straddleATickTrajectory = new HoverATickEncoderProcessorEvaluationTrajectory(encoderCountsPerMeter);


      jointTrajectories.add(dipDownTrajectory);
      jointTrajectories.add(sawtoothTrajectory);
      jointTrajectories.add(bangBangTrajectory);
      jointTrajectories.add(bangBangTrajectory2);
      jointTrajectories.add(sinusoidalTrajectory);
      jointTrajectories.add(chirpTrajectory);
      jointTrajectories.add(multipleSinusoidTrajectory);
      jointTrajectories.add(multipleSinusoidTrajectory2);
      jointTrajectories.add(decayingTrajectory);
      jointTrajectories.add(straddleATickTrajectory);

      EncoderProcessorComparer comparer = new EncoderProcessorComparer(jointTrajectories, maxTime, dt, encoderCountsPerMeter);
      comparer.start();
   }
}
