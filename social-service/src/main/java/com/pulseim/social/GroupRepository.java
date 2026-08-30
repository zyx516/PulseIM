package com.pulseim.social;

import org.springframework.data.jpa.repository.JpaRepository;

interface GroupRepository extends JpaRepository<GroupEntity, String> {
}
