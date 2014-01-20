/*  

Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package models.asynchttp;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import models.utils.VarUtils;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfigBean;
/**
 * 
 * @author ypei
 *
 */
public final class NingClientFactory {
	private final AsyncHttpClient fastClient;
	private final AsyncHttpClient slowClient;

	public NingClientFactory() {
		AsyncHttpClient fastClient = null;
		AsyncHttpClient slowClient = null;

		try {
			// create and configure async http client
			AsyncHttpClientConfigBean configFastClient = new AsyncHttpClientConfigBean();
			configFastClient.setConnectionTimeOutInMs(VarUtils.NING_FASTCLIENT_CONNECTION_TIMEOUT_MS);
			configFastClient.setRequestTimeoutInMs(VarUtils.NING_FASTCLIENT_REQUEST_TIMEOUT_MS);
			fastClient = new AsyncHttpClient(configFastClient);
			
			AsyncHttpClientConfigBean configSlowClient = new AsyncHttpClientConfigBean();
			configSlowClient.setConnectionTimeOutInMs(VarUtils.NING_SLOWCLIENT_CONNECTION_TIMEOUT_MS);
			configSlowClient.setRequestTimeoutInMs(VarUtils.NING_SLOWCLIENT_REQUEST_TIMEOUT_MS);
			slowClient = new AsyncHttpClient(configSlowClient);
			
			disableCertificateVerification();
		} catch (Exception e) {
			models.utils.LogUtils.printLogError("ERROR IN AsyncHttpClientFactorySafe " +e.getLocalizedMessage());
		}
		
		this.fastClient = fastClient;
		this.slowClient = slowClient;
	}

	public AsyncHttpClient getFastClient() {
		return fastClient;
	}

	public AsyncHttpClient getSlowClient() {
		return slowClient;
	}

	private void disableCertificateVerification() throws KeyManagementException, NoSuchAlgorithmException {
		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[] { new CustomTrustManager() };

		// Install the all-trusting trust manager
		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustAllCerts, new SecureRandom());
		final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
		final HostnameVerifier verifier = new HostnameVerifier() {
			@Override
			public boolean verify(final String hostname, final SSLSession session) {
				return true;
			}
		};

		HttpsURLConnection.setDefaultHostnameVerifier(verifier);
	}

	/**
	 * class CustomTrustManager.
	 */
	private static class CustomTrustManager implements X509TrustManager {
		/**
		 * @return certificate.
		 */
		public X509Certificate[] getAcceptedIssuers() {
			return (X509Certificate[]) null;
		}

		/**
		 * @param certs
		 * @param authType
		 */
		public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
			/* no op */
		}

		/**
		 * @param certs
		 * @param authType
		 */
		public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
			/* no op */
		}
	}
}
