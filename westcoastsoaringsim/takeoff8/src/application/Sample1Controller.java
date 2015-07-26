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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import application.Loop.AutopilotMode;
import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class Sample1Controller implements Initializable {
    @FXML
    private HBox hbox_main;
    @FXML
    AnchorPane ap_main;

    @FXML
    private Canvas canvas;

    @FXML
    private TextArea licenseinfo;

    @FXML
    private TextArea disclaimer;

    @FXML
    private Button btn_loadPilot;

    @FXML
    private ToggleButton toggle_autoPG;

    @FXML
    private ToggleButton toggle_autoFMU;

    @FXML
    private ChoiceBox<String> cbox_wind;

    @FXML
    private Button btn_img;

    @FXML
    private ToggleButton toggle_data;

    @FXML
    private Canvas datacanvas;

    @FXML
    private Slider slider_targetXdot;

    @FXML
    private Button bttn_targetXdot_snapshot;

    @FXML
    private AnchorPane ap_dataplot;

    AnimationTimer animationTimer;

    public Canvas getCanvas() {
	return canvas;
    }

    protected ObservableList<String> windmodes = FXCollections.observableArrayList("off", "horizontal", "vertical",
	"turbulent");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
	try {
	    Loop loop = new Loop();
	    licenseinfo.setText(Texts.licenseInfo);
		 
	    disclaimer.setText(
		Texts.disclaimer);

	    btn_loadPilot.setOnMouseClicked(evt -> {
		FileChooser fc = new FileChooser();
		File defaultDir = defaultDir();
		if (defaultDir != null && defaultDir.exists())
		    fc.setInitialDirectory(defaultDir);
		fc.setTitle("load pilot - take off");
		fc.getExtensionFilters().add(new ExtensionFilter("funtional mockup unit", "*.fmu"));
		File selectedFile = fc.showOpenDialog(this.getCanvas().getScene().getWindow());

		if (selectedFile != null) {
		    loop.flight.setFmuFile(selectedFile);
		    toggle_autoFMU.setDisable(false);
		}
	    });
	    btn_img.setOnMouseClicked(evt -> {
		DirectoryChooser dc = new DirectoryChooser();
		File defaultDir = defaultDir();
		if (defaultDir != null && defaultDir.exists())
		    dc.setInitialDirectory(defaultDir);
		dc.setTitle("load images - take off");
		File selectedFile = dc.showDialog(this.getCanvas().getScene().getWindow());
		if (selectedFile != null) {
		    loop.loadImages(selectedFile.toURI());
		}
	    });

	    slider_targetXdot.setMin(-30.0);
	    slider_targetXdot.setMax(50.0);
	    slider_targetXdot.setValue(20.0);
	    slider_targetXdot.valueProperty().addListener(invalid -> {
		loop.flight.targetXdot = slider_targetXdot.getValue();
	    });
	    bttn_targetXdot_snapshot.setOnMouseClicked(mouseEvt -> {
		slider_targetXdot.setValue(loop.flight.app_windX);
	    });
	    toggle_data.setSelected(true);
	    toggle_data.setOnMouseClicked(event -> {
		if (toggle_data.isSelected()) {
		    hbox_main.getChildren().add(ap_dataplot);
		} else {
		    if (!hbox_main.getChildren().remove(ap_dataplot)) {
			throw new RuntimeException("could not remove ap_dataplot, not found");
		    }
		    hbox_main.requestLayout();
		}
	    });

	    ToggleGroup toggleGroup = new ToggleGroup();
	    toggleGroup.getToggles().addAll(toggle_autoPG, toggle_autoFMU);
	    toggle_autoFMU.setDisable(true);
	    toggle_autoPG.setOnMouseClicked(event -> {
		if (toggle_autoPG.isSelected()) {
		    loop.flight.autoPilot = AutopilotMode.pg;
		} else {
		    loop.flight.autoPilot = AutopilotMode.off;
		}
	    });
	    toggle_autoFMU.setOnMouseClicked(event -> {
		if (toggle_autoFMU.isSelected()) {
		    loop.flight.autoPilot = AutopilotMode.fmu;
		} else {
		    loop.flight.autoPilot = AutopilotMode.off;
		}
	    });

	    canvas.addEventFilter(MouseEvent.ANY, (e) -> canvas.requestFocus());
	    cbox_wind.setItems(windmodes);
	    cbox_wind.setValue("off");
	    cbox_wind.getSelectionModel().selectedItemProperty().addListener(invalid -> {
		if (cbox_wind.getSelectionModel().getSelectedItem().equals("horizontal")) {
		    loop.weather.windMode = 2;
		} else if (cbox_wind.getSelectionModel().getSelectedItem().equals("vertical")) {
		    loop.weather.windMode = 1;
		} else if (cbox_wind.getSelectionModel().getSelectedItem().equals("turbulent")) {
		    loop.weather.windMode = 3;
		} else if (cbox_wind.getSelectionModel().getSelectedItem().equals("off")) {
		    loop.weather.windMode = 0;
		}
	    });

	    loop.initialize(canvas, datacanvas);

	    canvas.setFocusTraversable(true);
	    EventHandler<KeyEvent> eh = loop.createKeyEventHandler(slider_targetXdot,toggle_data);
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
		    //System.out.println(String.format("mouse moved (%s,%s)", event.getX(), event.getY()));
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
	} catch (IOException | URISyntaxException e) {
	    e.printStackTrace();
	}
    }

    private File defaultDir() {
	File defaultDir;
	URL main = Main.class.getResource("Main.class");
	if ("file".equalsIgnoreCase(main.getProtocol()))
	    defaultDir = new File(main.getPath()).getParentFile();
	else
	    defaultDir = new File("C:\\temp");
	if (!defaultDir.exists())
	    defaultDir = new File("D:\\temp");
	if (!defaultDir.exists())
	    defaultDir = null;
	return defaultDir;
    }

}
