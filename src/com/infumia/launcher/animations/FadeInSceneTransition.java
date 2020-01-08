package com.infumia.launcher.animations;

import javafx.animation.FadeTransition;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class FadeInSceneTransition {

    double xOffset = 0;
    double yOffset = 0;

    private Stage stage;
    private Scene tScreen;
    private Duration duration;
    private double lasty = -1;

    public FadeInSceneTransition(Stage stage, Scene tScreen, Duration duration) {
        this.stage = stage;
        this.tScreen = tScreen;
        this.duration = duration;
    }

    public void play() {
        FadeTransition fadeOutTransition;
        AnchorPane anchorPane = (AnchorPane) stage.getScene().lookup("#anchorpane");
        int size = anchorPane.getChildren().size();
        for (int i = 0; i < size; i++) {
            if (anchorPane.getChildren().get(i).getTypeSelector().equals("FontAwesomeIcon")) continue;
            if (anchorPane.getChildren().get(i).getTypeSelector().equals("Rectangle")) continue;
            fadeOutTransition = Animation.fadeIn(duration, anchorPane.getChildren().get(i));
            fadeOutTransition.setFromValue(0);
            fadeOutTransition.setToValue(1);
            fadeOutTransition.play();
        }
    }

    public void loadNextScene() {
        try {
            Parent parent = tScreen.getRoot();

            parent.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }
            });
            parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                }
            });
            parent.setOnDragDone((e) -> {
                if (stage.getY() < 0) {
                    stage.setY(0);
                }
                stage.setOpacity(1.0f);
            });
            parent.setOnMouseReleased((e) -> {
                if (stage.getY() < 0) {
                    stage.setY(0);
                }
                stage.setOpacity(1.0f);
            });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void makeStageDrageable() {
        Parent parent = tScreen.getRoot();

        parent.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getY() <= 70) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                    lasty = event.getY();
                }
            }
        });
        parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (lasty != -1 && lasty <= 70) {
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                }
            }
        });
        parent.setOnDragDone((e) -> {
            if (stage.getY() < 0) {
                stage.setY(0);
            }
            stage.setOpacity(1.0f);
            lasty = -1;
        });
        parent.setOnMouseReleased((e) -> {
            if (stage.getY() < 0) {
                stage.setY(0);
            }
            stage.setOpacity(1.0f);
            lasty = -1;
        });
    }
}
