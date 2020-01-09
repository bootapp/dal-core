package com.bootapp.core.repository;

import com.bootapp.core.domain.Feedback;
import com.bootapp.core.domain.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
