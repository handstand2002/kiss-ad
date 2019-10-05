# Kiss-AD

Web Scraper and downloader specifically tailored to use a particular anime site. Utilizes Kafka for communicating between microservices.

## Technologies

 - **Kafka**: messaging between services
 - **Avro**: Serializing and deserializing messages between services
 - **Confluent Schema Registry**: Message validation for backwards/forwards compatibility
 - **Kafka Streams**: global tables used to turn a kafka topic into a Map
 - **Aria2**: download files into filesystem (magnet and direct links)
 - **Spring**: Framework for services

## Kafka Topics
 - **ad.show.store**: interest list of shows
 - **ad.show.queue**: trigger for **hs-show-fetch** to check for new episodes of a show
 - **ad.episode.queue**: trigger for **download-delegator** to initiate download
 - **ad.episode.store**: store of downloaded episodes
 - **download-instruction**: send trigger to **downloader** to start download
 - **download-status**: status of active downloads are reported to this topic

## Services
 - **show-api**: REST API for modifying the interest list of shows to watch and download
 - **scheduler**: On defined schedules, sends trigger to **hs-show-fetch** to check for new episodes
 - **hs-show-fetch**: Scrapes a show page for all episodes. For any episodes not downloaded, sends message to **download-delegator**
 - **download-delegator**: Determines which link of an episode (based on configured quality settings) to send to downloader, sends
 - **downloader**: Uses Aria2 to download into filesystem

## Service: show-api
REST API for modifying the interest list of shows to watch and download. 

### yml properties

    messaging:  
      state-dir: 
      schema-registry-url: 
      brokers: 
    server:  
      port: 

### Kafka Topics
 - **ad.show.store**: interest list of shows

## Service: scheduler
On defined schedules, sends trigger to **hs-show-fetch** to check for new episodes. If a show is supposed to have a new episode but doesn't yet, will keep checking for new episode using a configured interval.

### yml properties

    messaging:  
      state-dir:
      schema-registry-url: 
      brokers: 
    delayed-episode-poll-frequency: < for shows that should have an episode released but don't yet, how frequently to re-check. e.g. 1h or 20m >

### Kafka Topics
 - **ad.show.store**: interest list of shows
 - **ad.show.queue**: trigger for hs-show-fetch to check for new episodes of a show
 - **ad.episode.queue**: trigger this module uses to check that a new episode was grabbed

## Service: hs-show-fetch
Scrapes a show page for all episodes. For any episodes not downloaded, sends message to download

### yml properties
    messaging:  
      state-dir:
      schema-registry-url:
      brokers:
    com.horriblesubs:  
      url:   
      get-shows-endpoint:
  
### Kafka Topics
 - **ad.show.queue**: trigger for hs-show-fetch to check for new episodes of a show
 - **ad.episode.store**: store of downloaded episodes
 - **ad.episode.queue**: trigger for **download-delegator** to use to initiate the download process

## Service: download-delegator
Receives requests to download episodes, picks correct quality based on preferences and sends the download to the downloader

### yml properties
    messaging:  
      schema-registry-url:
      brokers:

### Kafka Topics
 - **ad.episode.queue**: receive trigger start download
 - **download-instruction**: send trigger to **downloader** to start download

## Service: downloader
Uses Aria2 to download into filesystem
### yml properties
    messaging:  
      schema-registry-url:
      brokers:
    download:  
      downloader-id: 1  
      download-folder-root: "C:\\Downloads"  
      overwrite-permission: true  
      aria:  
        temp-download-dir: "C:\\tmp"  
        port: <RPC port for Downloader to communicate with child process Aria2>
        path: "C:\\Program Files\\Aria2\\aria2c.exe"  
        status-poll-interval: 5s  
        inactivity-timeout: 10m

### Kafka Topics
 - **ad.show.store**: interest list of shows
 - **download-instruction**: send trigger to **downloader** to start download
 - **download-status**: status of active downloads are reported to this topic at intervals defined in download.aria.status-poll-interval 

