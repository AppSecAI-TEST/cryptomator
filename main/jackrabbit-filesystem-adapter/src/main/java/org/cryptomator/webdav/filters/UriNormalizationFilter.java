package org.cryptomator.webdav.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Depending on the HTTP method a "/" is added to or removed from the end of an URI.
 * For example <code>MKCOL</code> creates a directory (ending on "/"), while <code>PUT</code> creates a file (not ending on "/").
 */
public class UriNormalizationFilter implements HttpFilter {

	private static final String[] FILE_METHODS = {"PUT"};
	private static final String[] DIRECTORY_METHODS = {"MKCOL"};
	private static final String MOVE = "MOVE";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (ArrayUtils.contains(FILE_METHODS, request.getMethod().toUpperCase())) {
			chain.doFilter(new FileUriRequest(request), response);
		} else if (ArrayUtils.contains(DIRECTORY_METHODS, request.getMethod().toUpperCase())) {
			chain.doFilter(new DirectoryUriRequest(request), response);
		} else if (MOVE.equalsIgnoreCase(request.getMethod())) {
			chain.doFilter(new CanonicalMoveRequest(request), response);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// no-op
	}

	/**
	 * Makes the destination header end on "/" if moving a directory and remove additional "/" if moving a file.
	 */
	private static class CanonicalMoveRequest extends HttpServletRequestWrapper {

		private static String DESTINATION_HEADER = "Destination";

		public CanonicalMoveRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getHeader(String name) {
			if (name.equalsIgnoreCase(DESTINATION_HEADER)) {
				return sameSuffixAsUri(super.getHeader(name));
			} else {
				return super.getHeader(name);
			}
		}

		private String sameSuffixAsUri(String str) {
			final String uri = this.getRequestURI();
			if (uri.endsWith("/")) {
				return StringUtils.appendIfMissing(str, "/");
			} else {
				return StringUtils.removeEnd(str, "/");
			}
		}

	}

	/**
	 * HTTP request, whose URI never ends on "/".
	 */
	private static class FileUriRequest extends HttpServletRequestWrapper {

		public FileUriRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			return StringUtils.removeEnd(super.getRequestURI(), "/");
		}

	}

	/**
	 * HTTP request, whose URI always ends on "/".
	 */
	private static class DirectoryUriRequest extends HttpServletRequestWrapper {

		public DirectoryUriRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			return StringUtils.appendIfMissing(super.getRequestURI(), "/");
		}

	}

}
