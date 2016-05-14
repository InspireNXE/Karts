/**
 * The MIT License (MIT)
 *
 * Copyright (c) InspireNXE <https://www.inspirenxe.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.inspirenxe.karts.track.generation;

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.gen.GenerationPopulator;

public final class FixedSizeCylinderBarrierGenerator implements GenerationPopulator {
    private final double radiusX, radiusZ;

    public FixedSizeCylinderBarrierGenerator(double radiusX, double radiusZ) {
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
    }

    @Override
    public void populate(World world, MutableBlockVolume buffer, ImmutableBiomeArea biomes) {
        final int xMin = buffer.getBlockMin().getX();
        final int xMax = buffer.getBlockMax().getX();
        final int zMin = buffer.getBlockMin().getZ();
        final int zMax = buffer.getBlockMax().getZ();
        if (xMax < -radiusX || zMax < -radiusZ || xMin > radiusX || zMin > radiusZ) {
            return;
        }
        final double xradiusSquared = radiusX * radiusX;
        final double zradiusSquared = radiusZ * radiusZ;
        final double oneLess = 1 - (1.7 / Math.max(radiusX, radiusZ));
        for (int x = buffer.getBlockMin().getX(); x <= buffer.getBlockMax().getX(); x++) {
            for (int z = buffer.getBlockMin().getZ(); z <= buffer.getBlockMax().getZ(); z++) {
                double dist = (x * x) / xradiusSquared + (z * z) / zradiusSquared;
                if(dist <= 1) {
                    buffer.setBlockType(x, 64, z, BlockTypes.ICE);

                    if (dist > oneLess) {
                        buffer.setBlockType(x, 65, z, BlockTypes.SNOW);
                    }
                }
            }
        }
    }
}
