package us.ihmc.plotting.plotter2d.frames;

import javax.vecmath.Vector2d;

public interface PlotterSpaceConverter
{
   public Vector2d getConversionToSpace(PlotterFrameSpace plotterFrameType);
}
