/*
Requirements for every page:
- PAGE_TOPIC constant, which defines the Stomp topic that emits updates for the page
- initPage() function, which is called when the page is initialized or on re-establishing WS connection
- updateUi(msg) function, which handles updating the UI with whatever message was received from server
 */
const PAGE_TOPIC = "/topic/shows"

function initPage() {
  $("#show-list").html("") // clear show table

  stompClient.publish({
    destination: "/app/shows/init",
    body: JSON.stringify({})
  });
  console.log("Published init msg");
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
  } else {
    console.log("Creating new row in UI");
    // create new row in UI
    let rowFull = "<tr id='show-listing-" + showId + "' data-enabled='" + isActive + "'>"
    rowFull += rowContents
    rowFull += "</tr>"

    $("#show-list").append(rowFull);
  }

  updateDisabledShows()
}

function checkShow(showId) {
  stompClient.publish({
    destination: "/app/show/check-new",
    body: JSON.stringify({'showId': showId})
  });
}