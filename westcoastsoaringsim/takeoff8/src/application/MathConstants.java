////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Paraglider Glider Simulation and Visualization Program
// by Peter Spear. peter_spear@telus.net
// Nov 25, 2011
//
//    Copyright (C) 2011  Peter Spear
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as
//    published by the Free Software Foundation, either version 3 of the
//    License, or (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU Affero General Public License for more details.
//
//    To see a full copy of the GNU Affero General Public License
//    see <http://www.gnu.org/licenses/>.
//
//
// The physics of this simulation is as accurate as possible.
// Polar Data came from XFLR5 Foil analysis program.
//		http://xflr5.sourceforge.net/xflr5.htm
// The foil and glider design came from the GNULAB2 work of
// 	Pere H. Casellas Laboratori d'envol
//		http://www.laboratoridenvol.com/projects/gnuLAB2/gnuLAB2.en.html
// Big thanks to the Processing.js guys! What a great visualization tool!
//		http://processingjs.org/
// Beats hell out of my original C program that just spewed numbers.
//
// Feel free to modify and tweek this design as much as you like.
// Note that it took quite a bit to get the glider to stall and recover
// in a realistic manner. It is quite sensitive to values of the lift and
// drag coeficients. It is easy to break it. The XFLR5 polar data is only
// used between -16 degrees and +25 degrees aoa. Outside this range values
// for lift and drag were guessed at and tweeked.
//
// Further note to tweekers. Paragliders are almost pitch un-stable. Reduce the
// glider drag just little and the glider will become unstable. Play with it
// for a bit and you will soon give up on the "pendulum stability" idea.
//
// Have fun. Tune up your active flying, work on your infinite tumble and try a loop-de-loop
//

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
package application;

public class MathConstants {
    public static double TWO_PI = 2.0 * Math.PI;
}
