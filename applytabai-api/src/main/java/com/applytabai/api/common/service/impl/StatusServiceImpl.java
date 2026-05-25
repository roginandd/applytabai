package com.applytabai.api.common.service.impl;

import com.applytabai.api.common.dto.StatusResponse;
import com.applytabai.api.common.service.StatusService;
import org.springframework.stereotype.Service;

@Service
public class StatusServiceImpl implements StatusService {

	@Override
	public StatusResponse status() {
		return new StatusResponse("applytabai-api", "phase1", "UP");
	}
}
