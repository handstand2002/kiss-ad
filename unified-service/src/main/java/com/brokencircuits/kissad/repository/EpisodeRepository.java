package com.brokencircuits.kissad.repository;

import com.brokencircuits.kissad.domain.EpisodeDto;
import com.brokencircuits.kissad.domain.EpisodeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<EpisodeDto, EpisodeId> {

}