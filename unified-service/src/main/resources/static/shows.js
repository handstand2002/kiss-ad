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