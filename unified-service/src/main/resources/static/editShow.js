/*
Requirements for every page:
- PAGE_TOPIC constant, which defines the Stomp topic that emits updates for the page
- initPage() function, which is called when the page is initialized or on re-establishing WS connection
- updateUi(msg) function, which handles updating the UI with whatever message was received from server
 */
const PAGE_TOPIC = null
var waitingForSaveAck = false

function initPage() {
  var urlParams = new URLSearchParams(window.location.search);
  let showId = urlParams.get('id')

  stompClient.publish({
    destination: "/app/requests",
    body: JSON.stringify({'type': "INIT", 'page': 'SHOW', 'params': {'id': showId}})
  });
  console.log("Published init msg");
}

function updateUi(msg) {
  if (msg.type === 'SHOW_LISTING') {
    console.log("Updating UI with msg: ", msg);
    if (!waitingForSaveAck) {
      populatePage(msg);
    } else {
      redirectBackToMainPage();
    }
  } else {
    console.error("Unsupported message type: ", msg);
  }
}

function redirectBackToMainPage() {
  window.location.href = "/shows.html";
}

function populatePage(msg) {
  console.log("Msg to populate page:", msg)
  $("#downloaded-ep-link").attr("href", "/showEpisodes.html?id=" + msg.id);
  $('#show-form [name="id"]').val(msg.id)
  $('#show-form [name="url"]').val(msg.url)
  $('#show-form [name="sourceName"]').val(msg.sourceName)
  $('#show-form [name="title"]').val(msg.title)
  $('#show-form [name="season"]').val(msg.season)
  $('#show-form [name="folderName"]').val(msg.folderName)
  $('#show-form [name="releaseScheduleCron"]').val(msg.releaseScheduleCron)
  $('#show-form [name="episodeNamePattern"]').val(msg.episodeNamePattern)
  $('#show-form [name="skipEpisodeString"]').val(msg.skipEpisodeString)
  $('#show-form [name="isActive"]').prop('checked', msg.isActive)

  // TODO: update select options dynamically
}

function submitChange() {

  let obj = {}
  let inputs = $('#show-form input,select')
  inputs.each(i => {
    let input = inputs[i]
    if (input.type == 'checkbox') {
      obj[input.name] = (input.value === 'on')
    } else {
      obj[input.name] = input.value
    }
  })
  obj.type = 'SHOW_LISTING'
  console.log(obj)
  waitingForSaveAck = true
  stompClient.publish({
    destination: "/app/requests",
    body: JSON.stringify({'type': "UPDATE_SHOW", 'details': obj})
  });
}

function confirmDelete(button) {
  if (confirm("Delete Show?")) {
    button.form.submit();
  }
}