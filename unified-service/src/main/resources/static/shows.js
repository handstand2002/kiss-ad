/*
Requirements for every page:
- PAGE_TOPIC constant, which defines the Stomp topic that emits updates for the page
- initPage() function, which is called when the page is initialized or on re-establishing WS connection
- updateUi(msg) function, which handles updating the UI with whatever message was received from server
 */
const PAGE_TOPIC = "/topic/shows"
const TOPIC_DOWNLOADER_STATUS = "/topic/downloader/status"

function initPage() {
  $("#show-list").html("") // clear show table

  stompClient.subscribe(TOPIC_DOWNLOADER_STATUS, handleDownloaderUpdate);

  stompClient.publish({
    destination: "/app/shows/init",
    body: JSON.stringify({})
  });
  console.log("Published init msg");
}

var progressBarMovementSchedule = null
function hideProgressBar() {
  if (progressBarPosition === -50) {
    return
  }
  if (progressBarMovementSchedule != null) {
    clearInterval(progressBarMovementSchedule)
    progressBarMovementSchedule = null
  }
  progressBarMovementSchedule = setInterval(() => moveProgressBar(-1), 10)
}

function showProgressBar() {
  if (progressBarPosition === 0) {
    return
  }
  if (progressBarMovementSchedule != null) {
    clearInterval(progressBarMovementSchedule)
    progressBarMovementSchedule = null
  }
  progressBarMovementSchedule = setInterval(() => moveProgressBar(1), 10)
}

var progressBarPosition = -50

function moveProgressBar(verticalMovement) {
  progressBarPosition += verticalMovement;
  if (progressBarPosition < -50) {
    progressBarPosition = -50;
  } else if (progressBarPosition > 0) {
    progressBarPosition = 0;
  }
  $("#download-bar").css({bottom: progressBarPosition});

  if (progressBarMovementSchedule != null && progressBarPosition === -50) {
    clearInterval(progressBarMovementSchedule)
  } else if (progressBarMovementSchedule != null && progressBarPosition === 0) {
    clearInterval(progressBarMovementSchedule)
  }
}

const ONE_KB = 1024
const ONE_MB = 1024 * 1024

function handleDownloaderUpdate(msg) {
  let body = JSON.parse(msg.body)

  let txt = body.filename
  txt += " - "
  let rateNumber
  let rateUnit
  let rateBytes = body.bytesPerSec
  if (rateBytes < ONE_KB) {
    rateNumber = rateBytes;
    rateUnit = "b"
  } else if (rateBytes < ONE_MB) {
    rateNumber = (rateBytes / ONE_KB).toFixed(2)
    rateUnit = "kb"
  } else {
    rateNumber = (rateBytes / ONE_MB).toFixed(2)
    rateUnit = "Mb"
  }
  txt += rateNumber + " " + rateUnit + "/s"

  $("#downloading-file-name").html(txt)
  $( "#progressbar" ).progressbar({
    value: (body.pcntComplete * 100)
  });

  if (body.complete) {
    hideProgressBar()
  } else {
    showProgressBar()
  }
}

function updateUi(msg) {
  if (msg.type === 'SHOW_LISTING') {
    console.log("Updating UI with msg: ", msg);
    upsertShow(msg);
  } else {
    console.error("Unsupported message type: ", msg);
  }
}

function updateDisabledShows() {
  let btn = document.getElementById("showHideShowsBox")

  let evalRow = function(row) {
    let showEnabledString = row.getAttribute("data-enabled")
    let showEnabled = showEnabledString != null && showEnabledString.toLowerCase() === "true";

    if (showEnabled || btn.checked) {
      row.hidden = false
    } else {
      row.hidden = true
    }
  };

  var rows = document.getElementById("show-list").rows
  for (let i = 0; i < rows.length; i++) {
    evalRow(rows[i])
  }
}

function toggleActivity(btn) {
  console.log(btn);
  btnLoc = btn.getBoundingClientRect()
  let y = btnLoc.top
  let x = btnLoc.left
  y = y + btnLoc.height

  let activityDiv = document.getElementById("recent-activity")
  activityDiv.style.marginTop = y + "px"
  activityDiv.style.marginLeft = x + "px"
  if (activityDiv.style.visibility == "hidden") {
    activityDiv.style.visibility = "visible"
  } else {
    activityDiv.style.visibility = "hidden"
  }
}

function upsertShow(msg) {
  let isActive = msg.isActive
  let showId = msg.id
  let showTitle = msg.title
  let nextEpTime = msg.nextEpisode
  let secondsToNextCheck = msg.secondsToNextCheck

  // First column
  let rowContents = "<td>"
  rowContents += "<a href='/show.html?id=" + showId + "'>" + showTitle + "</a>"
  rowContents += "<span class='next-episode-time'>" + nextEpTime + "</span>"
  rowContents += "</td>"

  // 2nd column
  rowContents += "<td>"
  rowContents += "<span><a href='javascript:void(0)' onclick='checkShow(\"" + showId + "\")'>チエック</a></span>"
  rowContents += "</td>"

  let existingListings = $("#show-listing-" + showId)
  console.log("Listings for show " + showId, existingListings)
  if (existingListings.length > 0) {
    console.log("Updating existing row:", existingListings)
    // row already exists in UI, update it instead of creating new one
    existingListings.html(rowContents);
    existingListings.attr("data-enabled", isActive);
  } else {
    console.log("Creating new row in UI");
    // create new row in UI
    let rowFull = "<tr id='show-listing-" + showId + "' data-enabled='" + isActive + "' data-seconds-to-next-check='" + secondsToNextCheck+ "'>"
    rowFull += rowContents
    rowFull += "</tr>"

    $("#show-list").append(rowFull);
  }

  sortList();
  updateDisabledShows()
}

function sortList() {
  let allRows = $("#show-list tr")
  allRows.each(i => allRows[i].remove())

  allRows.sort((a, b) => {
    let aSeconds = parseInt(a.getAttribute("data-seconds-to-next-check"))
    let bSeconds = parseInt(b.getAttribute("data-seconds-to-next-check"))
    return aSeconds - bSeconds
  });

  let list = $("#show-list")
  allRows.each(i => list.append(allRows[i]))
}

function checkShow(showId) {
  stompClient.publish({
    destination: "/app/show/check-new",
    body: JSON.stringify({'showId': showId})
  });
}