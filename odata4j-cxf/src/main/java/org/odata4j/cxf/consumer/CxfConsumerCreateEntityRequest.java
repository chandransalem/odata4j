package org.odata4j.cxf.consumer;

import org.apache.http.HttpResponse;
import org.core4j.Enumerable;
import org.odata4j.consumer.AbstractConsumerEntityPayloadRequest;
import org.odata4j.consumer.ODataServerException;
import org.odata4j.consumer.ODataClientRequest;
import org.odata4j.core.OCreateRequest;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.core.Throwables;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.format.Entry;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatParserFactory;
import org.odata4j.format.FormatType;
import org.odata4j.format.Settings;
import org.odata4j.internal.FeedCustomizationMapping;
import org.odata4j.internal.InternalUtil;

class CxfConsumerCreateEntityRequest<T> extends AbstractConsumerEntityPayloadRequest implements OCreateRequest<T> {

  private final FormatType formatType;
  private OEntity parent;
  private String navProperty;

  private final FeedCustomizationMapping fcMapping;

  CxfConsumerCreateEntityRequest(FormatType formatType, String serviceRootUri, EdmDataServices metadata, String entitySetName, FeedCustomizationMapping fcMapping) {
    super(entitySetName, serviceRootUri, metadata);
    this.formatType = formatType;
    this.fcMapping = fcMapping;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T execute() throws ODataServerException {
    ODataCxfClient client = new ODataCxfClient(this.formatType);
    try {
      EdmEntitySet ees = metadata.getEdmEntitySet(entitySetName);
      Entry entry = client.createRequestEntry(ees, null, props, links);

      StringBuilder url = new StringBuilder(serviceRootUri);
      if (parent != null) {
        url.append(InternalUtil.getEntityRelId(parent))
            .append("/")
            .append(navProperty);
      } else {
        url.append(entitySetName);
      }

      ODataClientRequest request = ODataClientRequest.post(url.toString(), entry);
      HttpResponse response = client.createEntity(request);

      ODataVersion version = InternalUtil.getDataServiceVersion(response.getFirstHeader(ODataConstants.Headers.DATA_SERVICE_VERSION).getValue());

      FormatParser<Entry> parser = FormatParserFactory.getParser(Entry.class,
          client.getFormatType(), new Settings(version, metadata, entitySetName, null, fcMapping));
      entry = parser.parse(client.getFeedReader(response));

      return (T) entry.getEntity();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get() {
    EdmEntitySet entitySet = metadata.getEdmEntitySet(entitySetName);
    return (T) OEntities.createRequest(entitySet, props, links);
  }

  @Override
  public OCreateRequest<T> properties(OProperty<?>... props) {
    return super.properties(this, props);
  }

  @Override
  public OCreateRequest<T> properties(Iterable<OProperty<?>> props) {
    return super.properties(this, props);
  }

  @Override
  public OCreateRequest<T> link(String navProperty, OEntity target) {
    return super.link(this, navProperty, target);
  }

  @Override
  public OCreateRequest<T> link(String navProperty, OEntityKey targetKey) {
    return super.link(this, navProperty, targetKey);
  }

  @Override
  public OCreateRequest<T> addToRelation(OEntity parent, String navProperty) {
    if (parent == null || navProperty == null) {
      throw new IllegalArgumentException("please provide the parent and the navProperty");
    }

    this.parent = parent;
    this.navProperty = navProperty;
    return this;
  }

  @Override
  public OCreateRequest<T> inline(String navProperty, OEntity... entities) {
    return super.inline(this, navProperty, entities);
  }

  @Override
  public OCreateRequest<T> inline(String navProperty, Iterable<OEntity> entities) {
    return super.inline(this, navProperty, Enumerable.create(entities).toArray(OEntity.class));
  }

}
