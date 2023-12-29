package com.oneclicktech.spring.config;

import java.net.URI;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

public class CleanUrlRedirectStrategy extends DefaultRedirectStrategy {

	@Override
	public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
			throws ProtocolException {
		// TODO Auto-generated method stub
		return super.isRedirected(request, response, context);
	}

	@Override
	public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context)
			throws ProtocolException {
		return super.getLocationURI(request, response, context);
	}

	@Override
	protected URI createLocationURI(String location) throws ProtocolException {
		// TODO Auto-generated method stub
		location = StringUtils.trimToEmpty(location).replaceAll(" ", "%20");
		return super.createLocationURI(location);
	}

	@Override
	protected boolean isRedirectable(String method) {
		// TODO Auto-generated method stub
		return super.isRedirectable(method);
	}

	@Override
	public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context)
			throws ProtocolException {
		// TODO Auto-generated method stub
		return super.getRedirect(request, response, context);
	}

	 
}