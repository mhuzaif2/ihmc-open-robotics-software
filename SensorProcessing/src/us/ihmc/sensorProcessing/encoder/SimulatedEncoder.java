package us.ihmc.sensorProcessing.encoder;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.IntYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class SimulatedEncoder
{
   private final double encoderTicksPerUnitOfPosition;
   private DoubleYoVariable positionFromEncoder;
   private IntYoVariable encoderTicks;

   public SimulatedEncoder(double encoderTicksPerUnitOfPosition, String name, YoVariableRegistry parentRegistry)
   {
      if (encoderTicksPerUnitOfPosition <= 0.0)
         throw new RuntimeException("encoderTicksPerUnitOfPosition must be > 0.0");

      YoVariableRegistry registry = new YoVariableRegistry("simulatedEncoder_" + name);

      positionFromEncoder = new DoubleYoVariable("positionFromEncoder_" + name, registry);
      encoderTicks = new IntYoVariable("encoderTicks_" + name, registry);

      this.encoderTicksPerUnitOfPosition = encoderTicksPerUnitOfPosition;
      parentRegistry.addChild(registry);
   }

   public void setActualPosition(double actualPosition)
   {
      encoderTicks.set((int) Math.round(actualPosition * encoderTicksPerUnitOfPosition));
      positionFromEncoder.set(converTicksToDistance(encoderTicks.getIntegerValue()));
   }

   public double getPositionFromEncoder()
   {
      return positionFromEncoder.getDoubleValue();
   }

   public int getEncoderTicks()
   {
      return encoderTicks.getIntegerValue();
   }

   public double converTicksToDistance(int ticks)
   {
      return ((double) ticks) / encoderTicksPerUnitOfPosition;
   }

   public double converTicksToDistance(double ticks)
   {
      return ticks / encoderTicksPerUnitOfPosition;
   }

   public double getEncoderTicksPerUnitOfPosition()
   {
      return encoderTicksPerUnitOfPosition;
   }
}
