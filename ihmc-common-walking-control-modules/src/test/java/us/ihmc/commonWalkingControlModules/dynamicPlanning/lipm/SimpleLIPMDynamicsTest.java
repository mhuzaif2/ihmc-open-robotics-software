package us.ihmc.commonWalkingControlModules.dynamicPlanning.lipm;

import org.ejml.data.DenseMatrix64F;
import org.junit.Test;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.robotics.testing.JUnitTools;
import us.ihmc.trajectoryOptimization.DefaultDiscreteState;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class SimpleLIPMDynamicsTest
{
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testNextStateComputation()
   {
      double deltaT = 0.01;
      double mass = 10.0;
      double gravityZ = 9.81;

      double x = 5.0;
      double y = 1.0;
      double xDot = 1.5;
      double yDot = -2.5;

      double pX = 4.5;
      double pY = 1.5;

      double z = 1.0;

      double fz = mass * gravityZ;

      SimpleLIPMDynamics dynamics = new SimpleLIPMDynamics(deltaT, 1.0, gravityZ);

      DenseMatrix64F currentState = new DenseMatrix64F(4, 1);
      currentState.set(0, x);
      currentState.set(1, y);
      currentState.set(2, xDot);
      currentState.set(3, yDot);

      DenseMatrix64F currentControl = new DenseMatrix64F(2, 1);
      currentControl.set(0, pX);
      currentControl.set(1, pY);

      DenseMatrix64F constants = new DenseMatrix64F(0, 1);

      DenseMatrix64F nextState = new DenseMatrix64F(4, 1);

      dynamics.getNextState(DefaultDiscreteState.DEFAULT, currentState, currentControl, constants, nextState);

      assertEquals(4, dynamics.getStateVectorSize());
      assertEquals(2, dynamics.getControlVectorSize());

      double nextX = x + deltaT * xDot + 0.5 * deltaT * deltaT * (x - pX) * fz / (mass * z);
      double nextY = y + deltaT * yDot + 0.5 * deltaT * deltaT * (y - pY) * fz / (mass * z);
      double nextXDot = xDot + deltaT * (x - pX) * fz / (mass * z);
      double nextYDot = yDot + deltaT * (y - pY) * fz / (mass * z);

      DenseMatrix64F expectedNextState = new DenseMatrix64F(4, 1);
      expectedNextState.set(0, nextX);
      expectedNextState.set(1, nextY);
      expectedNextState.set(2, nextXDot);
      expectedNextState.set(3, nextYDot);

      JUnitTools.assertMatrixEquals(expectedNextState, nextState, 1e-10);

      assertEquals(x, currentState.get(0), 1e-10);
      assertEquals(y, currentState.get(1), 1e-10);
      assertEquals(xDot, currentState.get(2), 1e-10);
      assertEquals(yDot, currentState.get(3), 1e-10);

      assertEquals(pX, currentControl.get(0), 1e-10);
      assertEquals(pY, currentControl.get(1), 1e-10);

      Random random = new Random(10L);
      for (int i = 0; i < 100; i++)
      {
         x = RandomNumbers.nextDouble(random, 10.0);
         y = RandomNumbers.nextDouble(random, 10.0);
         xDot = RandomNumbers.nextDouble(random, 0, 10);
         yDot = RandomNumbers.nextDouble(random, 0, 10);

         pX = RandomNumbers.nextDouble(random, 10.0);
         pY = RandomNumbers.nextDouble(random, 10.0);

         currentState = new DenseMatrix64F(4, 1);
         currentState.set(0, x);
         currentState.set(1, y);
         currentState.set(2, xDot);
         currentState.set(3, yDot);

         currentControl = new DenseMatrix64F(2, 1);
         currentControl.set(0, pX);
         currentControl.set(1, pY);

         constants = new DenseMatrix64F(0, 1);

         nextState = new DenseMatrix64F(4, 1);

         dynamics.getNextState(DefaultDiscreteState.DEFAULT, currentState, currentControl, constants, nextState);

         assertEquals(4, dynamics.getStateVectorSize());
         assertEquals(2, dynamics.getControlVectorSize());

         nextX = x + deltaT * xDot + 0.5 * deltaT * deltaT * (x - pX) * fz / (mass * z);
         nextY = y + deltaT * yDot + 0.5 * deltaT * deltaT * (y - pY) * fz / (mass * z);
         nextXDot = xDot + deltaT * (x - pX) * fz / (mass * z);
         nextYDot = yDot + deltaT * (y - pY) * fz / (mass * z);

         expectedNextState = new DenseMatrix64F(4, 1);
         expectedNextState.set(0, nextX);
         expectedNextState.set(1, nextY);
         expectedNextState.set(2, nextXDot);
         expectedNextState.set(3, nextYDot);

         JUnitTools.assertMatrixEquals(expectedNextState, nextState, 1e-10);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testDynamicsStateGradient()
   {
      double deltaT = 0.01;
      double mass = 10.0;
      double gravityZ = 9.81;

      double x = 5.0;
      double y = 1.0;
      double xDot = 1.5;
      double yDot = -2.5;

      double pX = 4.5;
      double pY = 1.5;

      double fz = mass * gravityZ;

      double z = 1.0;

      SimpleLIPMDynamics dynamics = new SimpleLIPMDynamics(deltaT, z, gravityZ);

      DenseMatrix64F currentState = new DenseMatrix64F(4, 1);
      currentState.set(0, x);
      currentState.set(1, y);
      currentState.set(2, xDot);
      currentState.set(3, yDot);

      DenseMatrix64F currentControl = new DenseMatrix64F(2, 1);
      currentControl.set(0, pX);
      currentControl.set(1, pY);

      DenseMatrix64F constants = new DenseMatrix64F(0, 1);

      DenseMatrix64F dynamicsStateGradient = new DenseMatrix64F(4, 4);

      dynamics.getDynamicsStateGradient(DefaultDiscreteState.DEFAULT, currentState, currentControl, constants, dynamicsStateGradient);

      assertEquals(4, dynamics.getStateVectorSize());
      assertEquals(2, dynamics.getControlVectorSize());

      double f00 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
      double f02 = deltaT;
      double f11 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
      double f13 = deltaT;
      double f20 = deltaT * fz / (mass * z);
      double f22 = 1;
      double f31 = deltaT * fz / (mass * z);
      double f33 = 1;

      DenseMatrix64F expectedDynamicsStateGradient = new DenseMatrix64F(4, 4);
      expectedDynamicsStateGradient.set(0, 0, f00);
      expectedDynamicsStateGradient.set(0, 2, f02);
      expectedDynamicsStateGradient.set(1, 1, f11);
      expectedDynamicsStateGradient.set(1, 3, f13);
      expectedDynamicsStateGradient.set(2, 0, f20);
      expectedDynamicsStateGradient.set(2, 2, f22);
      expectedDynamicsStateGradient.set(3, 1, f31);
      expectedDynamicsStateGradient.set(3, 3, f33);

      JUnitTools.assertMatrixEquals(expectedDynamicsStateGradient, dynamicsStateGradient, 1e-10);

      assertEquals(x, currentState.get(0), 1e-10);
      assertEquals(y, currentState.get(1), 1e-10);
      assertEquals(xDot, currentState.get(2), 1e-10);
      assertEquals(yDot, currentState.get(3), 1e-10);

      assertEquals(pX, currentControl.get(0), 1e-10);
      assertEquals(pY, currentControl.get(1), 1e-10);

      Random random = new Random(10L);
      for (int i = 0; i < 100; i++)
      {
         x = RandomNumbers.nextDouble(random, 10.0);
         y = RandomNumbers.nextDouble(random, 10.0);
         xDot = RandomNumbers.nextDouble(random, 0, 10);
         yDot = RandomNumbers.nextDouble(random, 0, 10);

         pX = RandomNumbers.nextDouble(random, 10.0);
         pY = RandomNumbers.nextDouble(random, 10.0);

         currentState = new DenseMatrix64F(4, 1);
         currentState.set(0, x);
         currentState.set(1, y);
         currentState.set(2, xDot);
         currentState.set(3, yDot);

         currentControl = new DenseMatrix64F(2, 1);
         currentControl.set(0, pX);
         currentControl.set(1, pY);

         constants = new DenseMatrix64F(0, 1);

         dynamicsStateGradient = new DenseMatrix64F(4, 4);

         dynamics.getDynamicsStateGradient(DefaultDiscreteState.DEFAULT, currentState, currentControl, constants, dynamicsStateGradient);

         assertEquals(4, dynamics.getStateVectorSize());
         assertEquals(2, dynamics.getControlVectorSize());

         f00 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
         f02 = deltaT;
         f11 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
         f13 = deltaT;
         f20 = deltaT * fz / (mass * z);
         f22 = 1;
         f31 = deltaT * fz / (mass * z);
         f33 = 1;

         expectedDynamicsStateGradient = new DenseMatrix64F(4, 4);
         expectedDynamicsStateGradient.set(0, 0, f00);
         expectedDynamicsStateGradient.set(0, 2, f02);
         expectedDynamicsStateGradient.set(1, 1, f11);
         expectedDynamicsStateGradient.set(1, 3, f13);
         expectedDynamicsStateGradient.set(2, 0, f20);
         expectedDynamicsStateGradient.set(2, 2, f22);
         expectedDynamicsStateGradient.set(3, 1, f31);
         expectedDynamicsStateGradient.set(3, 3, f33);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testDynamicsControlGradient()
   {
      double deltaT = 0.01;
      double mass = 10.0;
      double gravityZ = 9.81;

      double x = 5.0;
      double y = 1.0;
      double xDot = 1.5;
      double yDot = -2.5;
      double zDot = 0.5;

      double pX = 4.5;
      double pY = 1.5;

      double fz = mass * gravityZ;
      double z = 1.0;


      SimpleLIPMDynamics dynamics = new SimpleLIPMDynamics(deltaT, z, gravityZ);

      DenseMatrix64F currentState = new DenseMatrix64F(4, 1);
      currentState.set(0, x);
      currentState.set(1, y);
      currentState.set(2, xDot);
      currentState.set(3, yDot);

      DenseMatrix64F currentControl = new DenseMatrix64F(2, 1);
      currentControl.set(0, pX);
      currentControl.set(1, pY);

      DenseMatrix64F constants = new DenseMatrix64F(0, 1);

      DenseMatrix64F dynamicsControlGradient = new DenseMatrix64F(4, 2);

      dynamics.getDynamicsControlGradient(DefaultDiscreteState.DEFAULT, currentState, currentControl, constants, dynamicsControlGradient);

      assertEquals(4, dynamics.getStateVectorSize());
      assertEquals(2, dynamics.getControlVectorSize());

      double f00 = -0.5 * deltaT * deltaT * fz / (mass * z);
      double f11 = -0.5 * deltaT * deltaT * fz / (mass * z);
      double f20 = -deltaT * fz / (mass * z);
      double f31 = -deltaT * fz / (mass * z);

      DenseMatrix64F expectedDynamicsControlGradient = new DenseMatrix64F(4, 2);
      expectedDynamicsControlGradient.set(0, 0, f00);
      expectedDynamicsControlGradient.set(1, 1, f11);
      expectedDynamicsControlGradient.set(2, 0, f20);
      expectedDynamicsControlGradient.set(3, 1, f31);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlGradient, dynamicsControlGradient, 1e-10);

      assertEquals(x, currentState.get(0), 1e-10);
      assertEquals(y, currentState.get(1), 1e-10);
      assertEquals(xDot, currentState.get(2), 1e-10);
      assertEquals(yDot, currentState.get(3), 1e-10);

      assertEquals(pX, currentControl.get(0), 1e-10);
      assertEquals(pY, currentControl.get(1), 1e-10);

      Random random = new Random(10L);
      for (int i = 0; i < 100; i++)
      {
         x = RandomNumbers.nextDouble(random, 10.0);
         y = RandomNumbers.nextDouble(random, 10.0);
         xDot = RandomNumbers.nextDouble(random, 0, 10);
         yDot = RandomNumbers.nextDouble(random, 0, 10);

         pX = RandomNumbers.nextDouble(random, 10.0);
         pY = RandomNumbers.nextDouble(random, 10.0);

         currentState = new DenseMatrix64F(4, 1);
         currentState.set(0, x);
         currentState.set(1, y);
         currentState.set(2, xDot);
         currentState.set(3, yDot);

         currentControl = new DenseMatrix64F(2, 1);
         currentControl.set(0, pX);
         currentControl.set(1, pY);

         constants = new DenseMatrix64F(0, 1);

         dynamicsControlGradient = new DenseMatrix64F(4, 2);

         dynamics.getDynamicsControlGradient(DefaultDiscreteState.DEFAULT, currentState, currentControl, constants, dynamicsControlGradient);

         assertEquals(4, dynamics.getStateVectorSize());
         assertEquals(2, dynamics.getControlVectorSize());

         f00 = -0.5 * deltaT * deltaT * fz / (mass * z);
         f11 = -0.5 * deltaT * deltaT * fz / (mass * z);
         f20 = -deltaT * fz / (mass * z);
         f31 = -deltaT * fz / (mass * z);

         expectedDynamicsControlGradient = new DenseMatrix64F(4, 2);
         expectedDynamicsControlGradient.set(0, 0, f00);
         expectedDynamicsControlGradient.set(1, 1, f11);
         expectedDynamicsControlGradient.set(2, 0, f20);
         expectedDynamicsControlGradient.set(3, 1, f31);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlGradient, dynamicsControlGradient, 1e-10);
      }
   }


   /*
   @ContinuousIntegrationTest(estimatedDuration = 0.3)
   @Test(timeout = 30000)
   public void testDynamicsStateHessian()
   {
      double deltaT = 0.01;
      double mass = 10.0;
      double gravityZ = 9.81;

      double x = 5.0;
      double y = 1.0;
      double z = 1.2;
      double xDot = 1.5;
      double yDot = -2.5;
      double zDot = 0.5;

      double pX = 4.5;
      double pY = 1.5;
      double fz = 80;

      LIPMDynamics dynamics = new LIPMDynamics(deltaT, mass, gravityZ);

      DenseMatrix64F currentState = new DenseMatrix64F(6, 1);
      currentState.set(0, x);
      currentState.set(1, y);
      currentState.set(2, z);
      currentState.set(3, xDot);
      currentState.set(4, yDot);
      currentState.set(5, zDot);

      DenseMatrix64F currentControl = new DenseMatrix64F(3, 1);
      currentControl.set(0, pX);
      currentControl.set(1, pY);
      currentControl.set(2, fz);


      assertEquals(6, dynamics.getStateVectorSize());
      assertEquals(3, dynamics.getControlVectorSize());

      //double f00 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
      //double f02 = -0.5 * deltaT * deltaT * (x - pX) * fz * mass / (Math.pow(mass * z, 2.0));
      //double f11 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
      //double f12 = -0.5 * deltaT * deltaT * (y - pY) * fz * mass / (Math.pow(mass * z, 2.0));
      //double f30 = deltaT * fz / (mass * z);
      //double f32 = - deltaT * (x - pX) * mass * fz / Math.pow(mass * z, 2.0);
      //double f41 = deltaT * fz / (mass * z);
      //double f42 = - deltaT * (y - pY) * mass * fz / Math.pow(mass * z, 2.0);

      DenseMatrix64F dynamicsStateHessian = new DenseMatrix64F(6, 6);
      DenseMatrix64F expectedDynamicsStateHessian = new DenseMatrix64F(6, 6);

      dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 0, currentState, currentControl, dynamicsStateHessian);

      double f02 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
      double f32 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
      expectedDynamicsStateHessian.set(0, 2, f02);
      expectedDynamicsStateHessian.set(3, 2, f32);

      JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);



      dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 1, currentState, currentControl, dynamicsStateHessian);

      double f12 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
      double f42 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
      expectedDynamicsStateHessian.zero();
      expectedDynamicsStateHessian.set(1, 2, f12);
      expectedDynamicsStateHessian.set(4, 2, f42);

      JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);



      dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 2, currentState, currentControl, dynamicsStateHessian);

      double f00 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
      //double f02 = -0.5 * deltaT * deltaT * (x - pX) * fz * mass / (Math.pow(mass * z, 2.0));
      f02 = 0.5 * deltaT * deltaT * (x - pX) * fz * mass * 2 * mass * mass * z / (Math.pow(mass * z, 4.0));
      double f11 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
      f12 = 0.5 * deltaT * deltaT * (y - pY) * fz * mass * 2 * mass * mass * z / (Math.pow(mass * z, 4.0));
      double f30 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
      //double f32 = - deltaT * (x - pX) * mass * fz / Math.pow(mass * z, 2.0);
      f32 = deltaT * (x - pX) * mass * fz * 2 * mass * mass * z / Math.pow(mass * z, 4.0);
      double f41 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
      f42 = deltaT * (y - pY) * mass * fz * 2 * mass * mass * z / Math.pow(mass * z, 4.0);

      expectedDynamicsStateHessian.zero();
      expectedDynamicsStateHessian.set(0, 0, f00);
      expectedDynamicsStateHessian.set(0, 2, f02);
      expectedDynamicsStateHessian.set(1, 1, f11);
      expectedDynamicsStateHessian.set(1, 2, f12);
      expectedDynamicsStateHessian.set(3, 0, f30);
      expectedDynamicsStateHessian.set(3, 2, f32);
      expectedDynamicsStateHessian.set(4, 1, f41);
      expectedDynamicsStateHessian.set(4, 2, f42);

      JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);

      dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 3, currentState, currentControl, dynamicsStateHessian);
      expectedDynamicsStateHessian.zero();
      JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);

      dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 4, currentState, currentControl, dynamicsStateHessian);
      expectedDynamicsStateHessian.zero();
      JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);

      dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 5, currentState, currentControl, dynamicsStateHessian);
      expectedDynamicsStateHessian.zero();
      JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);


      assertEquals(x, currentState.get(0), 1e-10);
      assertEquals(y, currentState.get(1), 1e-10);
      assertEquals(z, currentState.get(2), 1e-10);
      assertEquals(xDot, currentState.get(3), 1e-10);
      assertEquals(yDot, currentState.get(4), 1e-10);
      assertEquals(zDot, currentState.get(5), 1e-10);

      assertEquals(pX, currentControl.get(0), 1e-10);
      assertEquals(pY, currentControl.get(1), 1e-10);
      assertEquals(fz, currentControl.get(2), 1e-10);

      Random random = new Random(10L);
      for (int i = 0; i < 100; i++)
      {
         x = RandomNumbers.nextDouble(random, 10.0);
         y = RandomNumbers.nextDouble(random, 10.0);
         z = RandomNumbers.nextDouble(random, 0, 5);
         xDot = RandomNumbers.nextDouble(random, 0, 10);
         yDot = RandomNumbers.nextDouble(random, 0, 10);
         zDot = RandomNumbers.nextDouble(random, 0, 10);

         pX = RandomNumbers.nextDouble(random, 10.0);
         pY = RandomNumbers.nextDouble(random, 10.0);
         fz = RandomNumbers.nextDouble(random, 0, 1000);

         currentState = new DenseMatrix64F(6, 1);
         currentState.set(0, x);
         currentState.set(1, y);
         currentState.set(2, z);
         currentState.set(3, xDot);
         currentState.set(4, yDot);
         currentState.set(5, zDot);

         currentControl = new DenseMatrix64F(3, 1);
         currentControl.set(0, pX);
         currentControl.set(1, pY);
         currentControl.set(2, fz);

         //double f00 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
         //double f02 = -0.5 * deltaT * deltaT * (x - pX) * fz * mass / (Math.pow(mass * z, 2.0));
         //double f11 = (1 + 0.5 * deltaT * deltaT * fz / (mass * z));
         //double f12 = -0.5 * deltaT * deltaT * (y - pY) * fz * mass / (Math.pow(mass * z, 2.0));
         //double f30 = deltaT * fz / (mass * z);
         //double f32 = - deltaT * (x - pX) * mass * fz / Math.pow(mass * z, 2.0);
         //double f41 = deltaT * fz / (mass * z);
         //double f42 = - deltaT * (y - pY) * mass * fz / Math.pow(mass * z, 2.0);

         dynamicsStateHessian = new DenseMatrix64F(6, 6);
         expectedDynamicsStateHessian = new DenseMatrix64F(6, 6);

         dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 0, currentState, currentControl, dynamicsStateHessian);

         f02 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f32 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
         expectedDynamicsStateHessian.set(0, 2, f02);
         expectedDynamicsStateHessian.set(3, 2, f32);

         JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);



         dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 1, currentState, currentControl, dynamicsStateHessian);

         f12 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f42 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
         expectedDynamicsStateHessian.zero();
         expectedDynamicsStateHessian.set(1, 2, f12);
         expectedDynamicsStateHessian.set(4, 2, f42);

         JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);



         dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 2, currentState, currentControl, dynamicsStateHessian);

         f00 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f02 = 0.5 * deltaT * deltaT * (x - pX) * fz * mass * 2 * mass * mass * z / (Math.pow(mass * z, 4.0));
         f11 = -0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f12 = 0.5 * deltaT * deltaT * (y - pY) * fz * mass * 2 * mass * mass * z / (Math.pow(mass * z, 4.0));
         f30 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f32 = deltaT * (x - pX) * mass * fz * 2 * mass * mass * z / Math.pow(mass * z, 4.0);
         f41 = -deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f42 = deltaT * (y - pY) * mass * fz * 2 * mass * mass * z / Math.pow(mass * z, 4.0);

         expectedDynamicsStateHessian.zero();
         expectedDynamicsStateHessian.set(0, 0, f00);
         expectedDynamicsStateHessian.set(0, 2, f02);
         expectedDynamicsStateHessian.set(1, 1, f11);
         expectedDynamicsStateHessian.set(1, 2, f12);
         expectedDynamicsStateHessian.set(3, 0, f30);
         expectedDynamicsStateHessian.set(3, 2, f32);
         expectedDynamicsStateHessian.set(4, 1, f41);
         expectedDynamicsStateHessian.set(4, 2, f42);

         JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);

         dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 3, currentState, currentControl, dynamicsStateHessian);
         expectedDynamicsStateHessian.zero();
         JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);

         dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 4, currentState, currentControl, dynamicsStateHessian);
         expectedDynamicsStateHessian.zero();
         JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);

         dynamics.getDynamicsStateHessian(DefaultDiscreteState.DEFAULT, 5, currentState, currentControl, dynamicsStateHessian);
         expectedDynamicsStateHessian.zero();
         JUnitTools.assertMatrixEquals(expectedDynamicsStateHessian, dynamicsStateHessian, 1e-10);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.3)
   @Test(timeout = 30000)
   public void testDynamicsControlHessian()
   {
      double deltaT = 0.01;
      double mass = 10.0;
      double gravityZ = 9.81;

      double x = 5.0;
      double y = 1.0;
      double z = 1.2;
      double xDot = 1.5;
      double yDot = -2.5;
      double zDot = 0.5;

      double pX = 4.5;
      double pY = 1.5;
      double fz = 80;

      LIPMDynamics dynamics = new LIPMDynamics(deltaT, mass, gravityZ);

      DenseMatrix64F currentState = new DenseMatrix64F(6, 1);
      currentState.set(0, x);
      currentState.set(1, y);
      currentState.set(2, z);
      currentState.set(3, xDot);
      currentState.set(4, yDot);
      currentState.set(5, zDot);

      DenseMatrix64F currentControl = new DenseMatrix64F(3, 1);
      currentControl.set(0, pX);
      currentControl.set(1, pY);
      currentControl.set(2, fz);

      assertEquals(6, dynamics.getStateVectorSize());
      assertEquals(3, dynamics.getControlVectorSize());

      DenseMatrix64F dynamicsControlHessian = new DenseMatrix64F(6, 3);

      dynamics.getDynamicsControlHessian(DefaultDiscreteState.DEFAULT, 0, currentState, currentControl, dynamicsControlHessian);

      //double f00 = -0.5 * deltaT * deltaT * fz / (mass * z);
      //double f02 = 0.5 * deltaT * deltaT * (x - pX) / (mass * z);
      //double f11 = -0.5 * deltaT * deltaT * fz / (mass * z);
      //double f12 = 0.5 * deltaT * deltaT * (y - pY) / (mass * z);
      //double f22 = 0.5 * deltaT * deltaT / mass;
      //double f30 = -deltaT * fz / (mass * z);
      //double f32 = deltaT * (x - pX) / (mass * z);
      //double f41 = -deltaT * fz / (mass * z);
      //double f42 = deltaT * (y - pY) / (mass * z);
      //double f52 = deltaT / mass;

      double f02 = -0.5 * deltaT * deltaT / (mass * z);
      double f32 = -deltaT / (mass * z);

      DenseMatrix64F expectedDynamicsControlHessian = new DenseMatrix64F(6, 3);
      expectedDynamicsControlHessian.set(0, 2, f02);
      expectedDynamicsControlHessian.set(3, 2, f32);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

      dynamics.getDynamicsControlHessian(DefaultDiscreteState.DEFAULT, 1, currentState, currentControl, dynamicsControlHessian);

      double f12 = -0.5 * deltaT * deltaT / (mass * z);
      double f42 = -deltaT / (mass * z);

      expectedDynamicsControlHessian.zero();
      expectedDynamicsControlHessian.set(1, 2, f12);
      expectedDynamicsControlHessian.set(4, 2, f42);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

      dynamics.getDynamicsControlHessian(DefaultDiscreteState.DEFAULT, 2, currentState, currentControl, dynamicsControlHessian);

      double f00 = -0.5 * deltaT * deltaT / (mass * z);
      double f11 = -0.5 * deltaT * deltaT / (mass * z);
      double f30 = -deltaT / (mass * z);
      double f41 = -deltaT / (mass * z);

      expectedDynamicsControlHessian.zero();
      expectedDynamicsControlHessian.set(0, 0, f00);
      expectedDynamicsControlHessian.set(1, 1, f11);
      expectedDynamicsControlHessian.set(3, 0, f30);
      expectedDynamicsControlHessian.set(4, 1, f41);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

      assertEquals(x, currentState.get(0), 1e-10);
      assertEquals(y, currentState.get(1), 1e-10);
      assertEquals(z, currentState.get(2), 1e-10);
      assertEquals(xDot, currentState.get(3), 1e-10);
      assertEquals(yDot, currentState.get(4), 1e-10);
      assertEquals(zDot, currentState.get(5), 1e-10);

      assertEquals(pX, currentControl.get(0), 1e-10);
      assertEquals(pY, currentControl.get(1), 1e-10);
      assertEquals(fz, currentControl.get(2), 1e-10);

      Random random = new Random(10L);
      for (int i = 0; i < 100; i++)
      {
         x = RandomNumbers.nextDouble(random, 10.0);
         y = RandomNumbers.nextDouble(random, 10.0);
         z = RandomNumbers.nextDouble(random, 0, 5);
         xDot = RandomNumbers.nextDouble(random, 0, 10);
         yDot = RandomNumbers.nextDouble(random, 0, 10);
         zDot = RandomNumbers.nextDouble(random, 0, 10);

         pX = RandomNumbers.nextDouble(random, 10.0);
         pY = RandomNumbers.nextDouble(random, 10.0);
         fz = RandomNumbers.nextDouble(random, 0, 1000);

         currentState = new DenseMatrix64F(6, 1);
         currentState.set(0, x);
         currentState.set(1, y);
         currentState.set(2, z);
         currentState.set(3, xDot);
         currentState.set(4, yDot);
         currentState.set(5, zDot);

         currentControl = new DenseMatrix64F(3, 1);
         currentControl.set(0, pX);
         currentControl.set(1, pY);
         currentControl.set(2, fz);

         dynamicsControlHessian = new DenseMatrix64F(6, 3);

         dynamics.getDynamicsControlHessian(DefaultDiscreteState.DEFAULT, 0, currentState, currentControl, dynamicsControlHessian);

         //double f00 = -0.5 * deltaT * deltaT * fz / (mass * z);
         //double f02 = 0.5 * deltaT * deltaT * (x - pX) / (mass * z);
         //double f11 = -0.5 * deltaT * deltaT * fz / (mass * z);
         //double f12 = 0.5 * deltaT * deltaT * (y - pY) / (mass * z);
         //double f22 = 0.5 * deltaT * deltaT / mass;
         //double f30 = -deltaT * fz / (mass * z);
         //double f32 = deltaT * (x - pX) / (mass * z);
         //double f41 = -deltaT * fz / (mass * z);
         //double f42 = deltaT * (y - pY) / (mass * z);
         //double f52 = deltaT / mass;

         f02 = -0.5 * deltaT * deltaT / (mass * z);
         f32 = -deltaT / (mass * z);

         expectedDynamicsControlHessian = new DenseMatrix64F(6, 3);
         expectedDynamicsControlHessian.set(0, 2, f02);
         expectedDynamicsControlHessian.set(3, 2, f32);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

         dynamics.getDynamicsControlHessian(DefaultDiscreteState.DEFAULT, 1, currentState, currentControl, dynamicsControlHessian);

         f12 = -0.5 * deltaT * deltaT / (mass * z);
         f42 = -deltaT / (mass * z);

         expectedDynamicsControlHessian.zero();
         expectedDynamicsControlHessian.set(1, 2, f12);
         expectedDynamicsControlHessian.set(4, 2, f42);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

         dynamics.getDynamicsControlHessian(DefaultDiscreteState.DEFAULT, 2, currentState, currentControl, dynamicsControlHessian);

         f00 = -0.5 * deltaT * deltaT / (mass * z);
         f11 = -0.5 * deltaT * deltaT / (mass * z);
         f30 = -deltaT / (mass * z);
         f41 = -deltaT / (mass * z);

         expectedDynamicsControlHessian.zero();
         expectedDynamicsControlHessian.set(0, 0, f00);
         expectedDynamicsControlHessian.set(1, 1, f11);
         expectedDynamicsControlHessian.set(3, 0, f30);
         expectedDynamicsControlHessian.set(4, 1, f41);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.3)
   @Test(timeout = 30000)
   public void testDynamicsStateGradientOfControlGradient()
   {
      double deltaT = 0.01;
      double mass = 10.0;
      double gravityZ = 9.81;

      double x = 5.0;
      double y = 1.0;
      double z = 1.2;
      double xDot = 1.5;
      double yDot = -2.5;
      double zDot = 0.5;

      double pX = 4.5;
      double pY = 1.5;
      double fz = 80;

      LIPMDynamics dynamics = new LIPMDynamics(deltaT, mass, gravityZ);

      DenseMatrix64F currentState = new DenseMatrix64F(6, 1);
      currentState.set(0, x);
      currentState.set(1, y);
      currentState.set(2, z);
      currentState.set(3, xDot);
      currentState.set(4, yDot);
      currentState.set(5, zDot);

      DenseMatrix64F currentControl = new DenseMatrix64F(3, 1);
      currentControl.set(0, pX);
      currentControl.set(1, pY);
      currentControl.set(2, fz);

      assertEquals(6, dynamics.getStateVectorSize());
      assertEquals(3, dynamics.getControlVectorSize());

      DenseMatrix64F dynamicsControlHessian = new DenseMatrix64F(6, 3);

      dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 0, currentState, currentControl, dynamicsControlHessian);

      //double f00 = -0.5 * deltaT * deltaT * fz / (mass * z);
      //double f02 = 0.5 * deltaT * deltaT * (x - pX) / (mass * z);
      //double f11 = -0.5 * deltaT * deltaT * fz / (mass * z);
      //double f12 = 0.5 * deltaT * deltaT * (y - pY) / (mass * z);
      //double f22 = 0.5 * deltaT * deltaT / mass;
      //double f30 = -deltaT * fz / (mass * z);
      //double f32 = deltaT * (x - pX) / (mass * z);
      //double f41 = -deltaT * fz / (mass * z);
      //double f42 = deltaT * (y - pY) / (mass * z);
      //double f52 = deltaT / mass;

      double f02 = 0.5 * deltaT * deltaT / (mass * z);
      double f32 = deltaT / (mass * z);

      DenseMatrix64F expectedDynamicsControlHessian = new DenseMatrix64F(6, 3);
      expectedDynamicsControlHessian.set(0, 2, f02);
      expectedDynamicsControlHessian.set(3, 2, f32);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);



      dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 1, currentState, currentControl, dynamicsControlHessian);

      double f12 = 0.5 * deltaT * deltaT / (mass * z);
      double f42 = deltaT / (mass * z);

      expectedDynamicsControlHessian.zero();
      expectedDynamicsControlHessian.set(1, 2, f12);
      expectedDynamicsControlHessian.set(4, 2, f42);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);



      dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 2, currentState, currentControl, dynamicsControlHessian);

      double f00 = 0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
      f02 = -0.5 * deltaT * deltaT * (x - pX) * mass / Math.pow(mass * z, 2.0);
      double f11 = 0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
      f12 = -0.5 * deltaT * deltaT * (y - pY) * mass / Math.pow(mass * z, 2.0);
      double f30 = deltaT * fz * mass / Math.pow(mass * z, 2.0);
      f32 = -deltaT * (x - pX) * mass / Math.pow(mass * z, 2.0);
      double f41 = deltaT * fz * mass / Math.pow(mass * z, 2.0);
      f42 = -deltaT * (y - pY) * mass / Math.pow(mass * z, 2.0);

      expectedDynamicsControlHessian.zero();
      expectedDynamicsControlHessian.set(0, 0, f00);
      expectedDynamicsControlHessian.set(0, 2, f02);
      expectedDynamicsControlHessian.set(1, 1, f11);
      expectedDynamicsControlHessian.set(1, 2, f12);
      expectedDynamicsControlHessian.set(3, 0, f30);
      expectedDynamicsControlHessian.set(3, 2, f32);
      expectedDynamicsControlHessian.set(4, 1, f41);
      expectedDynamicsControlHessian.set(4, 2, f42);

      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);





      dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 3, currentState, currentControl, dynamicsControlHessian);
      expectedDynamicsControlHessian.zero();
      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

      dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 4, currentState, currentControl, dynamicsControlHessian);
      expectedDynamicsControlHessian.zero();
      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

      dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 5, currentState, currentControl, dynamicsControlHessian);
      expectedDynamicsControlHessian.zero();
      JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

      assertEquals(x, currentState.get(0), 1e-10);
      assertEquals(y, currentState.get(1), 1e-10);
      assertEquals(z, currentState.get(2), 1e-10);
      assertEquals(xDot, currentState.get(3), 1e-10);
      assertEquals(yDot, currentState.get(4), 1e-10);
      assertEquals(zDot, currentState.get(5), 1e-10);

      assertEquals(pX, currentControl.get(0), 1e-10);
      assertEquals(pY, currentControl.get(1), 1e-10);
      assertEquals(fz, currentControl.get(2), 1e-10);

      Random random = new Random(10L);
      for (int i = 0; i < 100; i++)
      {
         x = RandomNumbers.nextDouble(random, 10.0);
         y = RandomNumbers.nextDouble(random, 10.0);
         z = RandomNumbers.nextDouble(random, 0, 5);
         xDot = RandomNumbers.nextDouble(random, 0, 10);
         yDot = RandomNumbers.nextDouble(random, 0, 10);
         zDot = RandomNumbers.nextDouble(random, 0, 10);

         pX = RandomNumbers.nextDouble(random, 10.0);
         pY = RandomNumbers.nextDouble(random, 10.0);
         fz = RandomNumbers.nextDouble(random, 0, 1000);

         currentState = new DenseMatrix64F(6, 1);
         currentState.set(0, x);
         currentState.set(1, y);
         currentState.set(2, z);
         currentState.set(3, xDot);
         currentState.set(4, yDot);
         currentState.set(5, zDot);

         currentControl = new DenseMatrix64F(3, 1);
         currentControl.set(0, pX);
         currentControl.set(1, pY);
         currentControl.set(2, fz);

         dynamicsControlHessian = new DenseMatrix64F(6, 3);

         dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 0, currentState, currentControl, dynamicsControlHessian);

         //double f00 = -0.5 * deltaT * deltaT * fz / (mass * z);
         //double f02 = 0.5 * deltaT * deltaT * (x - pX) / (mass * z);
         //double f11 = -0.5 * deltaT * deltaT * fz / (mass * z);
         //double f12 = 0.5 * deltaT * deltaT * (y - pY) / (mass * z);
         //double f22 = 0.5 * deltaT * deltaT / mass;
         //double f30 = -deltaT * fz / (mass * z);
         //double f32 = deltaT * (x - pX) / (mass * z);
         //double f41 = -deltaT * fz / (mass * z);
         //double f42 = deltaT * (y - pY) / (mass * z);
         //double f52 = deltaT / mass;

         f02 = 0.5 * deltaT * deltaT / (mass * z);
         f32 = deltaT / (mass * z);

         expectedDynamicsControlHessian = new DenseMatrix64F(6, 3);
         expectedDynamicsControlHessian.set(0, 2, f02);
         expectedDynamicsControlHessian.set(3, 2, f32);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);



         dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 1, currentState, currentControl, dynamicsControlHessian);

         f12 = 0.5 * deltaT * deltaT / (mass * z);
         f42 = deltaT / (mass * z);

         expectedDynamicsControlHessian.zero();
         expectedDynamicsControlHessian.set(1, 2, f12);
         expectedDynamicsControlHessian.set(4, 2, f42);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);



         dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 2, currentState, currentControl, dynamicsControlHessian);

         f00 = 0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f02 = -0.5 * deltaT * deltaT * (x - pX) * mass / Math.pow(mass * z, 2.0);
         f11 = 0.5 * deltaT * deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f12 = -0.5 * deltaT * deltaT * (y - pY) * mass / Math.pow(mass * z, 2.0);
         f30 = deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f32 = -deltaT * (x - pX) * mass / Math.pow(mass * z, 2.0);
         f41 = deltaT * fz * mass / Math.pow(mass * z, 2.0);
         f42 = -deltaT * (y - pY) * mass / Math.pow(mass * z, 2.0);

         expectedDynamicsControlHessian.zero();
         expectedDynamicsControlHessian.set(0, 0, f00);
         expectedDynamicsControlHessian.set(0, 2, f02);
         expectedDynamicsControlHessian.set(1, 1, f11);
         expectedDynamicsControlHessian.set(1, 2, f12);
         expectedDynamicsControlHessian.set(3, 0, f30);
         expectedDynamicsControlHessian.set(3, 2, f32);
         expectedDynamicsControlHessian.set(4, 1, f41);
         expectedDynamicsControlHessian.set(4, 2, f42);

         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);





         dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 3, currentState, currentControl, dynamicsControlHessian);
         expectedDynamicsControlHessian.zero();
         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

         dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 4, currentState, currentControl, dynamicsControlHessian);
         expectedDynamicsControlHessian.zero();
         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);

         dynamics.getDynamicsStateGradientOfControlGradient(DefaultDiscreteState.DEFAULT, 5, currentState, currentControl, dynamicsControlHessian);
         expectedDynamicsControlHessian.zero();
         JUnitTools.assertMatrixEquals(expectedDynamicsControlHessian, dynamicsControlHessian, 1e-10);
      }
   }
   */
}
