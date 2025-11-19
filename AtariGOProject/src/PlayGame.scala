import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage


//choose if you'd like TUI or GUI

//-------------------------GUI----------------------------//
class GUIApp extends Application {
  override def start(primaryStage: Stage): Unit = {
    val fxmlLoader = new FXMLLoader(getClass.getResource("Controller.fxml"))
    val mainViewRoot: Parent = fxmlLoader.load()

    val scene = new Scene(mainViewRoot)
    primaryStage.setScene(scene)
    primaryStage.setTitle("Atari Go")
    primaryStage.show()
  }
}

//code below is used to exectue the game via GUI

object GUIApp {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[GUIApp], args: _*)
  }
}

//-------------------------TUI----------------------------//


//code below is used to exectue the game via TUI

object TUIApp extends App {
  TUI.mainMenuLoop(TUI.Config(
    boardSize = 5,
    captureGoal = 5,
    maxMoveTimeMillis = 15000
  ))
}