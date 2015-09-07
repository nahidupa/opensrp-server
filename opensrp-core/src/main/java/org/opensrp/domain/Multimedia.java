package org.opensrp.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;
import org.motechproject.model.MotechBaseDataObject;

@TypeDiscriminator("doc.type == 'Multimedia'")
public class Multimedia extends MotechBaseDataObject {

	@JsonProperty
	private String caseId;
	@JsonProperty
	private String providerId;
	@JsonProperty
	private String contentType;
	
	public Multimedia() {

	}
	public Multimedia( String caseId, String providerId, String contentType) {
		this.caseId = caseId;
		this.providerId  = providerId; 
		this.contentType = contentType;
	}

	public Multimedia withCaseId(String caseId) {
		this.caseId = caseId;
		return this;
	}
	public Multimedia withProviderId(String providerId) {
		this.providerId = providerId;
		return this;
	}

	public Multimedia withFileName(String contentType) {
		this.contentType = contentType;
		return this;
	}
	public String getCaseId() {
		return caseId;
	}
	public String getProviderId() {
		return providerId;
	}
	public String getFileName() {
		return contentType;
	}
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	public void setFileName(String contentType) {
		this.contentType = contentType;
	}
	
	@Override
	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this, o, "id");
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, "id");
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}