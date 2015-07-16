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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.AnimationTimer;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class Sample1Controller implements Initializable {
    @FXML
    private Canvas canvas;

    @FXML
    private TextArea licenseinfo;
    
    @FXML
    private TextArea disclaimer;
    
    @FXML
    private Button btn_loadPilot;
    
    AnimationTimer animationTimer;

    public Canvas getCanvas() {
	return canvas;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
	try {
	    Loop loop = new Loop();
	    licenseinfo.setText(
		"The artwork is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License by Peter Spear. The source code is based on a Paraglider Simulation and Visualization Program by Peter Spear, licensed under AGPL3.");
	   disclaimer.setText("SAFETY WARNING: This work is only meant as personal training to program control software using JavaFMI and JavaFX. It is not meant to train for real human flight. Flight physics are not validated, but ported from the original work of Peter Spear.");
	    
	   btn_loadPilot.setOnMouseClicked( evt -> {
	       FileChooser fc = new FileChooser();
	       fc.setTitle("load pilot");
	       fc.getExtensionFilters().add(new ExtensionFilter("funtional mockup unit", "*.fmu"));
	       File selectedFile = fc.showOpenDialog(this.getCanvas().getScene().getWindow());
	       if (selectedFile!= null){
		   // todo: load as pilot into loop
	       }
	   });
	    
	 
	    
	    loop.initialize(canvas);

	    canvas.setFocusTraversable(true);
	    EventHandler<KeyEvent> eh = loop.createKeyEventHandler();
	    canvas.setOnKeyTyped(eh);
	    canvas.setOnMouseClicked(new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {
		    System.out.println(String.format("mouse clicked (%s,%s)", event.getX(), event.getY()));

		}
	    });
	    canvas.setOnMouseMoved(new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {
		    System.out.println(String.format("mouse moved (%s,%s)", event.getX(), event.getY()));
		    loop.mouseYProperty().set(event.getY());
		}
	    });
	    animationTimer = new AnimationTimer() {

		@Override
		public void handle(long now_ns) {
		    loop.draw();

		}

	    };
	    final long start_ns = System.nanoTime();
	    animationTimer.start();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

}
