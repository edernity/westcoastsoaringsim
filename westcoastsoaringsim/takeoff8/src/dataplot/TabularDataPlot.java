/* 
 * Copyright (c) 2015 Dennis Eder
 * This work is based on a paraglider simulation and visualization game by Peter Spear,
 * Director of the Westcoast Soaring Club in Canada, originally written in ProcessingJS 
 * (see http://www3.telus.net/cschwab/simPG/simGlider.pde), 
 * as announced here: http://www.paraglidingforum.com/viewtopic.php?t=43670 in 2011.
 * The goal is to port his game to JavaFX and use JavaFMI from SIANI/Spain to modularize 
 * pilot, glider and weather models showcasing FMI/FMU interoperability in a fun way.
 * It allows the user to design and exchange custom models and compare against others and even manual flight.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  To see a full copy of the GNU Affero General Public License
 *  see <http://www.gnu.org/licenses/>.
 *
 * Contributors: 
 *  Dennis Eder - port of Peter Spear's Javascript-based paraglider simulator (http://www3.telus.net/cschwab/simPG/simGlider.pde) to JavaFX, some rapid drafts to offer my team a debuggable environment and jumpstart into single-day hackfest
 */
package dataplot;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public class TabularDataPlot {
    GraphicsContext gc;
    private Canvas canvas;
    final double y_offset_initial = 20.0;
    double y_offset;
    double rowHeight = 12.0;
    double rowGap = 5.0;

    public TabularDataPlot(Canvas canvas) {
	if (canvas == null){
	    throw new RuntimeException("canvas is null");
	}
	this.canvas = canvas;
	this.gc = canvas.getGraphicsContext2D();
    }

    public void figure(double value, double min, double max, String label) {
	gc.setFill(Color.GREEN);
	gc.fillRect(0, y_offset, ((value - min) / (max - min)) * 200.0, rowHeight);
	gc.setFill(Color.WHITE);
	gc.fillText(String.format("%s: %.3f", label, value), 0, y_offset + rowHeight);
	y_offset += rowHeight + rowGap;
    }

    public void clear() {
	double height = canvas.getHeight();
	double width = canvas.getWidth();
	gc.setTransform(new Affine());
	gc.setFill(Color.BLACK);
	gc.fillRect(0, 0, width, height);
	y_offset = y_offset_initial;
    }
}
