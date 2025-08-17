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
    destination: "/app/requests",
    body: JSON.stringify({'type': "INIT", 'page': 'SHOWS'})
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
  rowContents += "<span><a href='/checkShow/" + showId + "'>チエック</a></span>"
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

/*
<tr data-enabled="false">
            <td>
              <span>
                <a href="/checkShow/ab610d04-41a6-4f24-9923-f72aee4701ad">チエック</a>
              </span>
            </td>
          </tr>
 */

//<table id="show-list" class="table table-striped">
//           <tbody>
//           <tr th:each="show : ${shows}" th:data-enabled="${show.isActive}">
//             <td>
//               <a th:href="@{/show/{id}(id=${show.id})}" th:text="${show.title}"></a>
//               <span th:text="${show.nextEpisode}" class="next-episode-time"></span>
//             </td>
//             <td>
//               <span>
//                 <a th:href="@{/checkShow/{id}(id=${show.id})}">チエック</a>
//               </span>
//             </td>
//           </tr>
//           </tbody>
//         </table>