/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.dataprovider.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.dspace.xoai.dataprovider.exceptions.BadArgumentException;
import org.dspace.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.dataprovider.exceptions.DoesNotSupportSetsException;
import org.dspace.xoai.dataprovider.exceptions.HandlerException;
import org.dspace.xoai.dataprovider.exceptions.NoMatchesException;
import org.dspace.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.handlers.helpers.ItemHelper;
import org.dspace.xoai.dataprovider.handlers.helpers.ItemRepositoryHelper;
import org.dspace.xoai.dataprovider.handlers.helpers.PreconditionHelper;
import org.dspace.xoai.dataprovider.handlers.helpers.ResumptionTokenHelper;
import org.dspace.xoai.dataprovider.handlers.helpers.SetRepositoryHelper;
import org.dspace.xoai.dataprovider.handlers.results.ListItemsResults;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.parameters.OAICompiledRequest;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.model.oaipmh.About;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.model.oaipmh.ListRecords;
import org.dspace.xoai.model.oaipmh.Metadata;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.model.oaipmh.ResumptionToken;
import org.dspace.xoai.xml.XSLPipeline;
import org.dspace.xoai.xml.XmlWriter;

import com.lyncode.xml.exceptions.XmlWriteException;

public class ListRecordsHandler extends VerbHandler<ListRecords> {
  private static Logger log = Logger.getLogger(ListRecordsHandler.class);
  private final ItemRepositoryHelper itemRepositoryHelper;
  private final SetRepositoryHelper setRepositoryHelper;

  public ListRecordsHandler(Context context, Repository repository) {
    super(context, repository);
    this.itemRepositoryHelper = new ItemRepositoryHelper(getRepository().getItemRepository());
    this.setRepositoryHelper = new SetRepositoryHelper(getRepository().getSetRepository());
  }

  @Override
  public ListRecords handle(OAICompiledRequest parameters) throws OAIException, HandlerException {
    ListRecords res = new ListRecords();
    int length = getRepository().getConfiguration().getMaxListRecords();

    if (parameters.hasSet() && !getRepository().getSetRepository().supportSets())
      throw new DoesNotSupportSetsException();

    PreconditionHelper.checkMetadataFormat(getContext(), parameters.getMetadataPrefix());

    log.debug("Getting items from data source");
    int offset = getOffset(parameters);
    ListItemsResults result;
    if (!parameters.hasSet()) {
      if (parameters.hasFrom() && !parameters.hasUntil())
        result = itemRepositoryHelper.getItems(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getFrom(), parameters.getResumptionToken().getScrollId());
      else if (!parameters.hasFrom() && parameters.hasUntil())
        result = itemRepositoryHelper.getItemsUntil(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getUntil(), parameters.getResumptionToken().getScrollId());
      else if (parameters.hasFrom() && parameters.hasUntil())
        result = itemRepositoryHelper.getItems(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getFrom(), parameters.getUntil(), parameters.getResumptionToken().getScrollId());
      else
        result = itemRepositoryHelper.getItems(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getResumptionToken().getScrollId());
    } else {
      if (!setRepositoryHelper.exists(getContext(), parameters.getSet()))
        throw new NoMatchesException();
      if (parameters.hasFrom() && !parameters.hasUntil())
        result = itemRepositoryHelper.getItems(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getSet(), parameters.getFrom(), parameters.getResumptionToken().getScrollId());
      else if (!parameters.hasFrom() && parameters.hasUntil())
        result = itemRepositoryHelper.getItemsUntil(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getSet(), parameters.getUntil(), parameters.getResumptionToken().getScrollId());
      else if (parameters.hasFrom() && parameters.hasUntil())
        result = itemRepositoryHelper.getItems(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getSet(), parameters.getFrom(), parameters.getUntil(),
          parameters.getResumptionToken().getScrollId());
      else
        result = itemRepositoryHelper.getItems(getContext(), offset, length, parameters.getMetadataPrefix(),
          parameters.getSet(), parameters.getResumptionToken().getScrollId());
    }
    log.debug("Items retrieved from data source");

    List<Item> results = result.getResults();
    if (results.isEmpty())
      throw new NoMatchesException();
    log.debug("Now adding records to the OAI-PMH Output");
    for (Item i : results)
      res.withRecord(this.createRecord(parameters, i));

    ResumptionToken.Value currentResumptionToken = new ResumptionToken.Value();
    if (parameters.hasResumptionToken()) {
      currentResumptionToken = parameters.getResumptionToken();
    } else {
      currentResumptionToken = parameters.extractResumptionToken();
    }

    ResumptionTokenHelper resumptionTokenHelper = new ResumptionTokenHelper(currentResumptionToken,
      getRepository().getConfiguration().getMaxListRecords()).withTotalResults(result.getTotal());
    res.withResumptionToken(resumptionTokenHelper.resolve(result.hasMore(), result.getScrollId()));

    return res;
  }

  private int getOffset(OAICompiledRequest parameters) {
    if (!parameters.hasResumptionToken())
      return 0;
    if (parameters.getResumptionToken().getOffset() == null)
      return 0;
    return parameters.getResumptionToken().getOffset().intValue();
  }

  private Record createRecord(OAICompiledRequest parameters, Item item)
    throws BadArgumentException, OAIException, NoMetadataFormatsException, CannotDisseminateFormatException {
    MetadataFormat format = getContext().formatForPrefix(parameters.getMetadataPrefix());
    Header header = new Header();
    Record record = new Record().withHeader(header);
    header.withIdentifier(item.getIdentifier());

    ItemHelper itemHelperWrap = new ItemHelper(item);

    header.withDatestamp(item.getDatestamp());
    for (Set set : itemHelperWrap.getSets(getContext(), getRepository().getFilterResolver()))
      header.withSetSpec(set.getSpec());
    if (item.isDeleted())
      header.withStatus(Header.Status.DELETED);

    if (!item.isDeleted()) {
      Metadata metadata = null;
      try {
        if (getContext().hasTransformer()) {
          metadata = new Metadata(
            toPipeline(item).apply(getContext().getTransformer()).apply(format.getTransformer()).process());
        } else {
          metadata = new Metadata(toPipeline(item).apply(format.getTransformer()).process());
        }
      } catch (XMLStreamException e) {
        throw new OAIException(e);
      } catch (TransformerException e) {
        throw new OAIException(e);
      } catch (IOException e) {
        throw new OAIException(e);
      } catch (XmlWriteException e) {
        throw new OAIException(e);
      }

      record.withMetadata(metadata);

      log.debug("Outputting ItemAbout");
      if (item.getAbout() != null) {
        for (About about : item.getAbout()) {
          record.withAbout(about);
        }
      }
    }
    return record;
  }

  private XSLPipeline toPipeline(Item item) throws XmlWriteException, XMLStreamException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    XmlWriter writer = new XmlWriter(output);
    Metadata metadata = item.getMetadata();
    metadata.write(writer);
    writer.close();
    return new XSLPipeline(new ByteArrayInputStream(output.toByteArray()), true);
  }
}
