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
  if (showId == null) {
    console.log("New episode page")
    autofillCronTime(document.getElementsByTagName("form")[0].releaseScheduleCron)
    $('#show-form [name="isActive"]').prop('checked', true)
    $("#header-show-title").html("New Show")
    return;
  }

  stompClient.publish({
    destination: "/app/show/init",
    body: JSON.stringify({'showId': showId})
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
  window.location.href = "shows.html";
}

function populatePage(msg) {
  console.log("Msg to populate page:", msg)
  $("#header-show-title").html(msg.title)
  $("#downloaded-ep-link").attr("href", "showEpisodes.html?id=" + msg.id);
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
      obj[input.name] = input.checked
    } else {
      obj[input.name] = input.value
    }
  })

  // for some reason, Jackson requires this property
  //  when deserializing the object into ShowListingMsg in Java
  obj.type = 'SHOW_LISTING'
  console.log(obj)
  waitingForSaveAck = true
  stompClient.publish({
    destination: "/app/show/update",
    body: JSON.stringify(obj)
  });
}