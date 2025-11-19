import Types._
import Stone._
import MyRandom._

//used for t7
case class GameState(
                      board: Board,
                      openCoords: List[Coord2D],
                      rng: MyRandom,
                      playerCaptured: Int,
                      computerCaptured: Int
                    )