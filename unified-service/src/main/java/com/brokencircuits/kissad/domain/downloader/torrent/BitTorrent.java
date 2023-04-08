package com.brokencircuits.kissad.domain.downloader.torrent;

import lombok.Value;

import java.util.List;

@Value
public class BitTorrent {

  List<List<String>> announceList;
}
