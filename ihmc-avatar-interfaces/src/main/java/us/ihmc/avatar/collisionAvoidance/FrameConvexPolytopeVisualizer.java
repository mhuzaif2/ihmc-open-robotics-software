package us.ihmc.avatar.collisionAvoidance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.ihmc.commons.MathTools;
import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.Axis;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.geometry.polytope.ConvexPolytopeConstructor;
import us.ihmc.geometry.polytope.DCELPolytope.Basics.ConvexPolytopeReadOnly;
import us.ihmc.geometry.polytope.DCELPolytope.Basics.PolytopeHalfEdgeReadOnly;
import us.ihmc.geometry.polytope.DCELPolytope.Basics.PolytopeVertexReadOnly;
import us.ihmc.geometry.polytope.DCELPolytope.Frame.FrameConvexPolytope;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearanceRGBColor;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicLineSegment;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class FrameConvexPolytopeVisualizer
{
   private final int numberOfVizEdges;
   private final int numberOfVizVertices;
   private final int numberOfCollisionVectors;
   private final YoVariableRegistry registry;
   private final YoGraphicsListRegistry graphicsListRegistry;
   private SimulationConstructionSet scs;
   private ArrayList<YoGraphicPosition> polytopeVerticesViz;
   private ArrayList<YoGraphicLineSegment> polytopeEdgesViz;
   private YoGraphicPosition position;
   private YoDouble yoTime;
   private final ConvexPolytopeReadOnly[] polytopes;
   private final Color[] polytopeColors;
   private int numberOfPolytopes = 0;
   private ArrayList<YoGraphicLineSegment> collisionVectors;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private boolean keepSCSUp = true;
   private int collisionVectorIndex = 0;
   private int iterationCount = 0;
   private boolean block = false;

   public FrameConvexPolytopeVisualizer(int maxNumberOfPolytopes, YoVariableRegistry registry, YoGraphicsListRegistry graphicsListRegistry)
   {
      this.scs = null;
      this.registry = registry;
      this.graphicsListRegistry = graphicsListRegistry;
      this.polytopes = new ConvexPolytopeReadOnly[maxNumberOfPolytopes];
      this.polytopeColors = new Color[maxNumberOfPolytopes];
      this.numberOfVizEdges = maxNumberOfPolytopes * 100;
      this.numberOfVizVertices = maxNumberOfPolytopes * 50;
      this.numberOfCollisionVectors = maxNumberOfPolytopes * 1;
      createVizArrays();
      createPolytopeVisualizationElements();
   }

   public FrameConvexPolytopeVisualizer(int maxNumberOfPolytopes, boolean keepSCSUp, Robot... robots)
   {
      this.registry = new YoVariableRegistry("PolytopeVisualizer");
      this.graphicsListRegistry = new YoGraphicsListRegistry();
      this.keepSCSUp = keepSCSUp;
      this.numberOfVizEdges = maxNumberOfPolytopes * 150;
      this.numberOfVizVertices = maxNumberOfPolytopes * 50;
      this.numberOfCollisionVectors = maxNumberOfPolytopes * 3;
      createVizArrays();
      polytopes = new ConvexPolytopeReadOnly[maxNumberOfPolytopes];
      polytopeColors = new Color[maxNumberOfPolytopes];
      createPolytopeVisualizationElements();
      setupSCS(robots);
   }

   private void createVizArrays()
   {
      polytopeVerticesViz = new ArrayList<>(numberOfVizVertices);
      polytopeEdgesViz = new ArrayList<>(numberOfVizEdges);
      collisionVectors = new ArrayList<>(numberOfCollisionVectors);
   }

   public void addPolytope(ConvexPolytopeReadOnly polytopeToAdd)
   {
      polytopes[numberOfPolytopes] = polytopeToAdd;
      polytopeColors[numberOfPolytopes] = getNextColor();
      numberOfPolytopes++;
      updateNonBlocking();
   }

   private Color getNextColor()
   {
      double numberOfDivisionsPerColor = Math.pow(polytopes.length, 1.0 / 3.0);
      double r = MathTools.clamp((numberOfPolytopes % numberOfDivisionsPerColor) / (numberOfDivisionsPerColor - 1), 0.0, 1.0);
      double g = MathTools.clamp(((numberOfPolytopes / numberOfDivisionsPerColor) % numberOfDivisionsPerColor) / (numberOfDivisionsPerColor - 1), 0.0, 1.0);
      double b = MathTools.clamp(((numberOfPolytopes / numberOfDivisionsPerColor / numberOfDivisionsPerColor) % numberOfDivisionsPerColor)
            / (numberOfDivisionsPerColor - 1), 0.0, 1.0);
      return new Color((float) r, (float) g, (float) b);
   }

   public void addPolytope(ConvexPolytopeReadOnly polytopeToAdd, Color color)
   {
      polytopes[numberOfPolytopes] = polytopeToAdd;
      polytopeColors[numberOfPolytopes] = color;
      numberOfPolytopes++;
   }

   public void update()
   {
      updatePolytopeVisualization(polytopes);
      if (scs != null && keepSCSUp)
      {
         PrintTools.debug("Sleeping forever");
         ThreadTools.sleepForever();
      }
   }

   public void updateNonBlocking()
   {
      updatePolytopeVisualization(polytopes);
   }

   private void setupSCS(Robot... robots)
   {
      Robot robot = new Robot(getClass().getSimpleName() + "Robot");
      yoTime = robot.getYoTime();
      robot.addYoVariableRegistry(registry);
      robot.addYoGraphicsListRegistry(graphicsListRegistry);
      SimulationConstructionSetParameters parameters = new SimulationConstructionSetParameters();
      Robot[] robotList;
      if (robots == null)
      {
         robotList = new Robot[1];
         robotList[0] = robot;
      }
      else
      {
         robotList = Arrays.copyOf(robots, robots.length + 1);
         robotList[robots.length] = robot;
      }
      scs = new SimulationConstructionSet(robotList, parameters);
      Graphics3DObject coordinateSystem = new Graphics3DObject();
      coordinateSystem.addCoordinateSystem(.5);
      scs.addStaticLinkGraphics(coordinateSystem);
      scs.setGroundVisible(false);
      scs.setDT(1.0, 1);
      scs.startOnAThread();
   }

   public void tickSCS()
   {
      if (scs != null)
      {
         yoTime.add(1.0);
         scs.tickAndUpdate();
      }
   }

   public void showCollisionVector(Point3D point1, Point3D point2)
   {
      this.collisionVectors.get(collisionVectorIndex++).setStartAndEnd(point1, point2);
   }

   public void clearCollisionVectors()
   {
      for (int i = 0; i < collisionVectorIndex; i++)
         this.collisionVectors.get(i).setToNaN();
      collisionVectorIndex = 0;
   }

   private YoGraphicLineSegment xVector;
   private YoGraphicLineSegment yVector;
   private YoGraphicLineSegment zVector;

   public void createPolytopeVisualizationElements()
   {
      xVector = new YoGraphicLineSegment("xAxis", "Viz", worldFrame, new YoAppearanceRGBColor(Color.PINK, 0.0), registry);
      xVector.setDrawArrowhead(true);
      yVector = new YoGraphicLineSegment("yAxis", "Viz", worldFrame, new YoAppearanceRGBColor(Color.PINK, 0.0), registry);
      yVector.setDrawArrowhead(true);
      zVector = new YoGraphicLineSegment("zAxis", "Viz", worldFrame, new YoAppearanceRGBColor(Color.PINK, 0.0), registry);
      zVector.setDrawArrowhead(true);
      graphicsListRegistry.registerYoGraphic("Axis", xVector);
      graphicsListRegistry.registerYoGraphic("Axis", yVector);
      graphicsListRegistry.registerYoGraphic("Axis", zVector);

      collisionVectors.clear();
      for (int i = 0; i < numberOfCollisionVectors; i++)
      {
         YoGraphicLineSegment vector = new YoGraphicLineSegment("CollisionVector" + i, "Viz", worldFrame, new YoAppearanceRGBColor(Color.RED, 0.0), registry);
         vector.setDrawArrowhead(true);
         vector.setToNaN();
         collisionVectors.add(vector);
      }
      graphicsListRegistry.registerYoGraphics("CollisionVectors", collisionVectors);

      polytopeEdgesViz.clear();
      for (int i = 0; i < numberOfVizEdges; i++)
      {
         YoGraphicLineSegment edge = new YoGraphicLineSegment("PolytopeEdge" + i, "Viz", worldFrame, new YoAppearanceRGBColor(Color.GRAY, 0.5), registry);
         edge.setDrawArrowhead(false);
         edge.setToNaN();
         polytopeEdgesViz.add(edge);
      }
      graphicsListRegistry.registerYoGraphics("PolytopeEdges", polytopeEdgesViz);

      for (int i = 0; i < numberOfVizVertices; i++)
      {
         YoGraphicPosition point = new YoGraphicPosition("PolytopeVertex" + i, "Viz", registry, 0.001, new YoAppearanceRGBColor(Color.GRAY, 0.0));
         point.setPositionToNaN();
         polytopeVerticesViz.add(point);
      }
      graphicsListRegistry.registerYoGraphics("PolytopeVertices", polytopeVerticesViz);

      position = new YoGraphicPosition("PositionForVisibleEdges", "Viz", registry, 0.1, new YoAppearanceRGBColor(Color.GRAY, 0.5));
      position.setPositionToNaN();
      graphicsListRegistry.registerYoGraphic("VisualPoint", position);
   }

   public void updatePolytopeVisualization(ConvexPolytopeReadOnly... polytopes)
   {
      int edgeIndex = 0;
      int vertexIndex = 0;
      for (int j = 0; j < numberOfPolytopes && polytopes[j] != null; j++)
      {
         Color color = polytopeColors[j];
         List<? extends PolytopeHalfEdgeReadOnly> edges = polytopes[j].getEdges();
         List<? extends PolytopeVertexReadOnly> vertices = polytopes[j].getVertices();
         int i = 0;
         for (i = 0; i < edges.size(); i++)
         {
            polytopeEdgesViz.get(edgeIndex).setStartAndEnd(edges.get(i).getOriginVertex().getPosition(), edges.get(i).getDestinationVertex().getPosition());
            polytopeEdgesViz.get(edgeIndex).getAppearance().getColor().set(color);
            edgeIndex++;
         }
         for (i = 0; i < vertices.size(); i++)
         {
            polytopeVerticesViz.get(vertexIndex).setPosition(vertices.get(i).getPosition());
            vertexIndex++;
         }
      }
      for (; edgeIndex < polytopeEdgesViz.size(); edgeIndex++)
         polytopeEdgesViz.get(edgeIndex).setToNaN();
      for (; vertexIndex < polytopeVerticesViz.size(); vertexIndex++)
         polytopeVerticesViz.get(vertexIndex).setPositionToNaN();
      tickSCS();
   }

   public void updateColor(FrameConvexPolytope polytopeToChange, Color newColor)
   {
      for (int i = 0; i < numberOfPolytopes; i++)
         if (polytopes[i] == polytopeToChange)
            polytopeColors[i] = newColor;
      updateNonBlocking();
   }

   public void removePolytope(ConvexPolytopeReadOnly polytope)
   {
      int i = 0;
      for (; i < numberOfPolytopes; i++)
      {
         if (polytopes[i] == polytope)
         {
            polytopes[i] = null;
            numberOfPolytopes--;
            break;
         }
      }
      for (; i < polytopes.length - 1; i++)
         polytopes[i] = polytopes[i + 1];
      polytopes[polytopes.length - 1] = null;
      update();
   }

   public void update(ConvexPolytopeReadOnly simplex)
   {
      for (int i = 0; i < numberOfPolytopes; i++)
      {
         if (polytopes[i] == simplex)
         {
            //PrintTools.debug("Vertices: " + simplex.getNumberOfVertices());
            updateNonBlocking();
            if (block && iterationCount++ > 10)
               throw new RuntimeException("This happened");
            return;
         }
      }
      addPolytope(simplex);
   }
}