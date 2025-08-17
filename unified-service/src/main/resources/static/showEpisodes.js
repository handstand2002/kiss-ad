/*
Requirements for every page:
- PAGE_TOPIC constant, which defines the Stomp topic that emits general updates for the page
- initPage() function, which is called when the page is initialized or on re-establishing WS connection
- updateUi(msg) function, which handles updating the UI with whatever message was received from server
 */
const PAGE_TOPIC = null
var SHOW_ID = null

function initPage() {
    var urlParams = new URLSearchParams(window.location.search);
    let showId = urlParams.get('id')
    SHOW_ID = showId;
    let showEpTopic = '/topic/shows/' + SHOW_ID
    stompClient.subscribe(showEpTopic, msg => populateEpisodeList(JSON.parse(msg.body)));

    stompClient.publish({
        destination: "/app/show-episodes/init",
        body: JSON.stringify({'showId': showId})
    });
    console.log("Published init msg");
}

function populateTitle(msg) {
    $("#show-title").html(msg.title)
}

function deleteEpisode(epNumber) {
    stompClient.publish({
        destination: "/app/show/episode/delete",
        body: JSON.stringify({'showId': SHOW_ID, 'episodeNumber': epNumber})
    });
}

function populateEpisodeList(msg) {

    console.log("Handling episode update:", msg)
    let downloadTime = msg.downloadTime
    let downloadedQuality = msg.downloadedQuality
    let episodeNumber = msg.episodeNumber
    let isDelete = msg.delete;
    let existingListings = $("#ep-listing-" + episodeNumber)
    if (isDelete) {
        existingListings.remove();
        console.log("Need to remove from page:", existingListings);
        return
    }

    // First column
    let rowContents = "<td>" + episodeNumber + "</td>"
    // 2nd column
    rowContents += "<td style='white-space: nowrap'>" + downloadTime + "</td>"
    // 3rd column
    rowContents += "<td>" + downloadedQuality + "</td>"
    // 4th column
    rowContents += "<td><a href='javascript:void(0)' onclick='deleteEpisode(" + episodeNumber +")'>消す</a></td>"

    if (existingListings.length > 0) {
        console.log("Updating existing row:", existingListings)
        // row already exists in UI, update it instead of creating new one
        existingListings.html(rowContents);
    } else {
        console.log("Creating new row in UI");
        // create new row in UI
        let rowFull = "<tr id='ep-listing-" + episodeNumber + "'>"
        rowFull += rowContents
        rowFull += "</tr>"

        $("#ep-list").append(rowFull);
    }
}

function updateUi(msg) {
    if (msg.type === 'SHOW_LISTING') {
        console.log("Updating UI with msg: ", msg);
        populateTitle(msg);
    } else if (msg.type === 'EPISODE_LISTING') {
        populateEpisodeList(msg);
    } else {
        console.error("Unsupported message type: ", msg);
    }
}
