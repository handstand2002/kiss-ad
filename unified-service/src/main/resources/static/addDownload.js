/*
Requirements for every page:
- PAGE_TOPIC constant, which defines the Stomp topic that emits updates for the page
- initPage() function, which is called when the page is initialized or on re-establishing WS connection
- updateUi(msg) function, which handles updating the UI with whatever message was received from server
 */
const PAGE_TOPIC = null;
const TOPIC_DOWNLOADER_STATUS = "/topic/downloader/status"
const WS_TOPIC_DOWNLOADER_SVC_UPDATE = "/topic/downloader-service/status";

function initPage() {

  stompClient.subscribe(WS_TOPIC_DOWNLOADER_SVC_UPDATE, handleServerMsg(updateDownloadSvc));
  // TODO: init page
  // stompClient.publish({
  //   destination: "/app/downloads/init",
  //   body: JSON.stringify({})
  // });
  console.log("Published init msg");
}

function handleServerMsg(fn) {
  return msg => {
    let parsed = JSON.parse(msg.body)
    fn(parsed);
  }
}

const ONE_KB = 1024
const ONE_MB = 1024 * 1024

function readableDataRate(rateBytes) {
  let rateNumber
  let rateUnit
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
  return rateNumber + " " + rateUnit + "/s"
}

function updateDownloadSvc(msg) {
  console.log("Download svc msg: ", msg);

  let rootGid = msg.rootGid;
  let title = msg.title;
  let completedLength = msg.completedLength;
  let connections = msg.connections;
  let downloadSpeed = msg.downloadSpeed;
  let files = msg.files;
  let numPieces = msg.numPieces;
  let numSeeders = msg.numSeeders;
  let totalLength = msg.totalLength;
  let uploadLength = msg.uploadLength;
  let uploadSpeed = msg.uploadSpeed;
  let readableDlRate = readableDataRate(downloadSpeed)

  let pcntDone = (completedLength / totalLength * 100);

  let headerContents = "<div style='display:inline-block; width: 100px' id='progress-" + rootGid + "'></div><div style='display: inline-block; width: 40%; margin-left: 10px'>" + title + " - " + pcntDone.toFixed(2) + "%"
  if (pcntDone < 100) {
    headerContents += " " + readableDlRate
  }
  headerContents += "</div>"

  let bodyContents = "<p>Mauris mauris ante, blandit et, ultrices a, suscipit eget, quam. Integer ut neque. Vivamus nisi metus, molestie vel, gravida in, condimentum sit amet, nunc. Nam a nibh. Donec suscipit eros. Nam mi. Proin viverra leo ut odio. Curabitur malesuada. Vestibulum a velit eu ante scelerisque vulputate.</p>"

  let existingHeader = $("#dl-listing-h3-" + rootGid)
  let table = $("#download-table");
  if (existingHeader.length > 0) {
    // row already exists in UI, update it instead of creating new one
    existingHeader.html(headerContents);
    $("#dl-listing-" + rootGid).html(bodyContents)
  } else {
    // create new row in UI
    let rowFull = "<h3 id='dl-listing-h3-" + rootGid + "' class='ui-accordion-header-collapsed'>"
    rowFull += headerContents
    rowFull += "</h3>"

    table.append(rowFull);

    let bodyFull = "<div id='dl-listing-" + rootGid + "'>"
    bodyFull += bodyContents
    bodyFull += "</div>"
    table.append(bodyFull);
  }

  let progressBar = $("#progress-" + rootGid);
  progressBar.progressbar({
    value: pcntDone
  });

  table.accordion( "refresh" );

  // TODO: sort list
  // sortList();
}

function initDownloadTable() {
  $( "#download-table" ).accordion({
    collapsible: true,
    active: false
  });
}

function saveDownload() {

  let downloadProps = {}
  let inputs = $('#newDownloadDialog form input,select')
  inputs.each(i => {
    let input = inputs[i]
    if (input.type == 'checkbox') {
      downloadProps[input.name] = input.checked
    } else {
      downloadProps[input.name] = input.value
    }
  })
  console.log("Saving download", downloadProps);
  stompClient.publish({
    destination: "/app/downloads/new",
    body: JSON.stringify(downloadProps)
  });
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


