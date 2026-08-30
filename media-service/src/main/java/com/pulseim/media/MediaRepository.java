package com.pulseim.media;

import org.springframework.data.jpa.repository.JpaRepository;

interface MediaRepository extends JpaRepository<MediaObjectEntity, String> {
}
