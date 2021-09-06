
class TaskState {
  constructor(position) {

    var map = new MapEntry("1", position, 0);
    var pending = new MapEntry("2", position, 1);
  }
}

class MapEntry extends algo.render.Rectangle {
  constructor(value, queuePosition, entryPosition) {
    super({
      opacity: 0,
      x: 5 + ((queuePosition %2) * 855),
      y: 5 + (Math.floor(queuePosition / 2) * 250) + entryPosition*90,
      w: 35,
      h: 40,
      text: value
    })
  }
}

var lastStreamTaskPosition = -1;
class StreamTask extends algo.render.Rectangle {

  constructor(value) {
    var position = ++lastStreamTaskPosition;

    var initialValues = {
      x: adjustForWidth(positionX(position), 100),
      y: adjustForHeight(positionY(position), 100),
      h: 135,
      w: 100,
      text: value
    };
    super(initialValues);
    new TaskState(position);
  }

}

class Message extends algo.render.Rectangle {
  constructor(value) {
    var initialValues = {
      x: 430,
      y: 200,
      h: 40,
      w: 40,
      text: value
    }
    super(initialValues);
  }

  move(position) {
    this.set({
      x: adjustForWidth(positionX(position), 40),
      y: adjustForHeight(positionY(position), 40)
    })
  }
}

function positionX(position) {
  return (position % 2) == 0 ? alignLeft() + 100 : alignRight() - 100;
}

function positionY(position) {
  return Math.floor(position / 2) == 0 ? alignTop() + 100 : alignBottom() - 100;
}

function addMarginRight(coordinate, margin) {
  return coordinate - margin;
}

function addMarginLeft(coordinate, margin) {
  return coordinate + margin;
}

function addMarginTop(coordinate, margin) {
  return coordinate + margin;
}

function addMarginBottom(coordinate, margin) {
  return coordinate - margin;
}

function alignCenterX() {
  return 450;
}

function alignCenterY() {
  return 278;
}

function alignLeft() {
  return 0;
}

function alignRight() {
  return 900;
}

function alignTop() {
  return 0;
}

function alignBottom() {
  return 556;
}

function adjustForWidth(coordinate, width) {
  return coordinate - (width/2)
}

function adjustForHeight(coordinate, height) {
  return coordinate - (height/2)
}

var algorithm = wrapGenerator.mark(function algorithm() {
    var WORD, varray, left, right, makeArray, message;

    return wrapGenerator(function algorithm$(context$1$0) {
        while (1) switch (context$1$0.prev = context$1$0.next) {
        case 128:

//          message = new Message("2");

          context$1$0.next = 129;

          return {
              step: "Creating message",
              line: "//=Create",
              variables: {
              }
          };
        case 129:

//          message.move(0);

          context$1$0.next = "end";

          return {
              step: "Updating message",
              line: "//=Move",
              variables: {
              }
          };
        case 0:

          rect = new StreamTask("T1");
          new StreamTask('T2');
          new StreamTask('T3');
          new StreamTask('T4');

          context$1$0.next = 128;
          return {
              step: "Initialized rectangle",
              line: "//=Initialize",
              variables: {
                  rect: rect
              }
          };

            makeArray = function makeArray() {

                // get bounds of surface we are displayed on
                var bounds = algo.BOUNDS;

                // derive tile size from surface size and word length
                var kS = bounds.w / (WORD.length + 2);

                // we only need a simple single row grid
                var layout = new algo.layout.GridLayout(bounds, 1, WORD.length);

                // create the array wrapper
                return new algo.core.Array({

                    // initialize with the word

                    data: WORD,

                    // called whenever a new item is added to the array, you should return the element used
                    // to visualize the item

                    createElement: _.bind(function(value, index) {

                        // create a new letter tile. Display the letter and the array index in the tile
                        var element = new algo.render.LetterTile({
                            text: value,
                            w: kS,
                            h: kS * 1.5,
                            value: index
                        });

                        // position within the appropriate row/column of the layout
                        element.layout(layout.getBox(0, index));

                        return element;

                    }, this),

                    // Use a path based animation to transition swapped elements to thier new locations
                    swapElement: _.bind(function(value, newIndex, oldIndex, element) {

                        // get the bounding box of the new and old cell
                        var newCell = layout.getBox(0, newIndex),
                            oldCell = layout.getBox(0, oldIndex);

                        // get x position for element centered in cell
                        var newX = newCell.cx - element.w / 2,
                            oldX = oldCell.cx - element.w / 2;

                        // height of the cell is used to move the items above or below the displayed string and
                        // the start/end vertical position of the tiles is a constant

                        var H = element.h * 1.5,
                            Y = element.y;

                        // if item was in the left/lower half of the array move it up and over,
                        // if item was in the right/upper half of the array move it down and under

                        var yoffset = oldIndex < WORD.length / 2 ? H : -H;

                        element.set({
                            y: [Y + yoffset, Y + yoffset, Y],
                            x: [oldX, newX, newX],
                            state: [algo.render.kS_BLUE, algo.render.kS_BLUE, algo.render.kS_FADED]
                        });
                    })
                });
            };

            WORD = "過ぎたるはなお及ばざるが如";
            varray = makeArray();
            left = 0, right = WORD.length - 1;
            context$1$0.next = 6;

            return {
                step: "The string we are going to reverse. Initialize two indices, left and right to either end of the array.",
                line: "//=Initialize",
                variables: {
                    Word: WORD,
                    left: left,
                    right: right
                }
            };
        case 6:
            if (!(left < right)) {
                context$1$0.next = 16;
                break;
            }

            context$1$0.next = 9;

            return {
                step: "Exchange the items in the slots identified by the left and right variables.",
                line: "//=swap",
                variables: {
                    left: left,
                    right: right
                }
            };
        case 9:
            // swap the two items on the array and reposition their elements
            varray.swap(left, right);

            context$1$0.next = 12;

            return {
                autoskip: true
            };
        case 12:
            // move left and right indices towards the center of the array
            left += 1;
            right -= 1;
            context$1$0.next = 6;
            break;
        case 16:
            context$1$0.next = 18;

            return {
                step: 'The algorithm is complete when the left and right indices either converge on the middle element or pass over each other.'
            };
        case 18:
        case "end":
            return context$1$0.stop();
        }
    }, algorithm, this);
});