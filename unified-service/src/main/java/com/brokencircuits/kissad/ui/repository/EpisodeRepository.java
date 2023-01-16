package com.brokencircuits.kissad.ui.repository;

import com.brokencircuits.kissad.ui.domain.EpisodeDto;
import com.brokencircuits.kissad.ui.domain.EpisodeId;
import com.brokencircuits.kissad.ui.domain.ShowDto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<EpisodeDto, EpisodeId> {

}