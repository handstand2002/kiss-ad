package com.brokencircuits.kissad.ui.downloader.domain.torrent;

import lombok.Value;

import java.util.List;

@Value
public class BitTorrent {

  List<List<String>> announceList;
}
