
import Types._
import Stone._


object Project {

  //T1

  def randomMove(lstOpenCoords: List[Coord2D], rand: MyRandom): (Coord2D, MyRandom) = {
    // playrandomly verifies that lstOpenCoords is not empty
    val (idx, newRng) = rand.nextInt(lstOpenCoords.length)
    (lstOpenCoords(idx), newRng)
  }


//T2
def play(board: Board, player: Stone, coord: Coord2D, lstOpenCoords: List[Coord2D]): (Option[Board], List[Coord2D]) = {
  coord match {
    case (x, y) =>
      if (!lstOpenCoords.contains(coord)) (None, lstOpenCoords)
      else {
        val updatedRow = board(x).updated(y, player)
        (Some(board.updated(x, updatedRow)), lstOpenCoords.filterNot(_ == coord))
        }
  }
}



//T3
def playRandomly(
                  board: Board,
                  r: MyRandom,
                  player: Stone,
                  lstOpenCoords: List[Coord2D],
                  f: (List[Coord2D], MyRandom) => (Coord2D, MyRandom),
                ): (Board, MyRandom, List[Coord2D]) = {

  val validMoves = lstOpenCoords.filterNot(coord => wouldCaptureSelf(board, player, coord))

  def tryMoves(remaining: List[Coord2D], rng: MyRandom): (Board, MyRandom, List[Coord2D]) = {
    if (remaining.isEmpty) (board, rng, lstOpenCoords)
    else {
      val (coord, nextRng) = f(remaining, rng)
      play(board, player, coord, lstOpenCoords) match {
        case (Some(newBoard), newOpenCoords) => (newBoard, nextRng, newOpenCoords)
        case (None, _) => tryMoves(remaining.filterNot(_ == coord), nextRng)
      }
    }
  }

  tryMoves(validMoves, r)
}


  // Used to get adjacent co ordinates(neighbours) of a coordinate
  // used in: collectGroup, hasEmptyNeighbor
  def getNeighbours(coord: Coord2D, boardSize: Int): List[Coord2D] = {
    val (x, y) = coord
    List((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)).filter {
      case (i, j) => i >= 0 && j >= 0 && i < boardSize && j < boardSize
    }
  }

  // Used to collect all connected stones of the same type starting from a coordinate
  // used in: processCaptureAtCoord, wouldCaptureSelf
  def collectGroup(board: Board, coord: Coord2D, visited: Set[Coord2D], target: Stone): Set[Coord2D] = {
    if (visited.contains(coord) || board(coord._1)(coord._2) != target) Set.empty
    else {
      val newVisited = visited + coord
      getNeighbours(coord, board.length).foldLeft(Set(coord)) { (group, nbr) =>
        group ++ collectGroup(board, nbr, newVisited ++ group, target)
      }
    }
  }

  // checks if any stone in the group has at least one empty neighbor
  // used in: processCaptureAtCoord, wouldCaptureSelf
  def hasEmptyNeighbor(board: Board, group: Set[Coord2D], boardSize: Int): Boolean = {
    group.exists { case (x, y) =>
      getNeighbours((x, y), boardSize).exists { case (nx, ny) =>
        board(nx)(ny) == Stone.Empty
      }
    }
  }

  // Used to set all removed stone to Empty
  // used in: processCaptureAtCoord
  def removeStones(board: Board, group: Set[Coord2D]): Board = {
    group.foldLeft(board) { case (b, (i, j)) =>
      b.updated(i, b(i).updated(j, Stone.Empty))
    }
  }

  // T5
  def captureGroupStones(board: Board, player: Stone): (Board, Int) = {
    val opponent = if (player == Stone.Black) Stone.White else Stone.Black
    captureAllGroups(generateAllCoords(board.length), board, opponent)
  }

  // used to processes a single coordinate to check if it leads to a capture
  // used in: captureAllGroup
  def processCaptureAtCoord(coord: Coord2D, visited: Set[Coord2D], accBoard: Board, captured: Int, target: Stone): (Board, Int, Set[Coord2D]) = {
    val group = collectGroup(accBoard, coord, visited, target)
    if (group.isEmpty || hasEmptyNeighbor(accBoard, group, accBoard.length)) {
      (accBoard, captured, visited ++ group)
    } else {
      val updatedBoard = removeStones(accBoard, group)
      (updatedBoard, captured + group.size, visited ++ group)
    }
  }

  // goes through all coordinates and captures opponent groups without an empty "neighbor"
  // used in: captureGroupStones
  def captureAllGroups(coords: List[Coord2D], board: Board, target: Stone): (Board, Int) = {

    def captureGroupsRecursively(pending: List[Coord2D], visited: Set[Coord2D], accBoard: Board, totalCaptured: Int): (Board, Int) = pending match {
      case Nil => (accBoard, totalCaptured)
      case head :: tail if visited.contains(head) =>
        captureGroupsRecursively(tail, visited, accBoard, totalCaptured)
      case head :: tail =>
        val (newBoard, newCaptured, newVisited) = processCaptureAtCoord(head, visited, accBoard, totalCaptured, target)
        captureGroupsRecursively(tail, newVisited, newBoard, newCaptured)
    }

    captureGroupsRecursively(coords, Set.empty, board, 0)
  }


  //used to  generate  coordinates for a board of given size
  def generateAllCoords(boardSize: Int): List[Coord2D] = {

    def generateCoordsRecursive(x: Int, y: Int, acc: List[Coord2D]): List[Coord2D] = {
      if (x >= boardSize) acc.reverse
      else if (y >= boardSize) generateCoordsRecursive(x + 1, 0, acc)
      else generateCoordsRecursive(x, y + 1, (x, y) :: acc)
    }

    generateCoordsRecursive(0, 0, Nil)
  }


  //used in play randomly to avoid playing in a place that will cause you to be captured
  def wouldCaptureSelf(board: Board, player: Stone, coord: Coord2D): Boolean = {
    if (board(coord._1)(coord._2) != Stone.Empty) return true

    val (x, y) = coord
    val updatedRow = board(x).updated(y, player)
    val tempBoard = board.updated(x, updatedRow)

    val (afterCapture, _) = captureGroupStones(tempBoard, player)
    val group = collectGroup(afterCapture, coord, Set.empty, player)

    !hasEmptyNeighbor(afterCapture, group, board.length)
  }

  // checks if either player has won
  //T6
  def checkWin(playerCaptured: Int, computerCaptured: Int, target: Int): String =
    if (playerCaptured >= target)
      "player"
    else if (computerCaptured >= target) "computer"
    else "ongoing"


  // used to updates list of open positions by removing any stones that were captured capture
  def updateOpenCoordsAfterCapture(oldBoard: Board, newBoard: Board, currentOpen: List[Coord2D]): List[Coord2D] = {
    def findNewlyFreedCoords(x: Int, y: Int, acc: List[Coord2D]): List[Coord2D] = {
      if (x >= oldBoard.length) acc
      else if (y >= oldBoard.length) findNewlyFreedCoords(x + 1, 0, acc)
      else {
        val freed = if (oldBoard(x)(y) != Stone.Empty && newBoard(x)(y) == Stone.Empty)
          (x, y) :: acc
        else acc
        findNewlyFreedCoords(x, y + 1, freed)
      }
    }

    (currentOpen ++ findNewlyFreedCoords(0, 0, Nil)).distinct
  }

}
