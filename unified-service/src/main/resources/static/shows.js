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