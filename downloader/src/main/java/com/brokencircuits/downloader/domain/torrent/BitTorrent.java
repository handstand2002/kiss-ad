package com.brokencircuits.downloader.domain.torrent;

import java.util.List;
import lombok.Value;

@Value
public class BitTorrent {

  private List<List<String>> announceList;
}
