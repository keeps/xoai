/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.dataprovider;

import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.From;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Identifier;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.MetadataPrefix;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.ResumptionToken;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Set;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Until;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Verb;

import org.apache.log4j.Logger;
import org.dspace.xoai.dataprovider.exceptions.BadArgumentException;
import org.dspace.xoai.dataprovider.exceptions.BadResumptionToken;
import org.dspace.xoai.dataprovider.exceptions.DuplicateDefinitionException;
import org.dspace.xoai.dataprovider.exceptions.HandlerException;
import org.dspace.xoai.dataprovider.exceptions.IllegalVerbException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.exceptions.UnknownParameterException;
import org.dspace.xoai.dataprovider.handlers.ErrorHandler;
import org.dspace.xoai.dataprovider.handlers.GetRecordHandler;
import org.dspace.xoai.dataprovider.handlers.IdentifyHandler;
import org.dspace.xoai.dataprovider.handlers.ListIdentifiersHandler;
import org.dspace.xoai.dataprovider.handlers.ListMetadataFormatsHandler;
import org.dspace.xoai.dataprovider.handlers.ListRecordsHandler;
import org.dspace.xoai.dataprovider.handlers.ListSetsHandler;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.parameters.OAICompiledRequest;
import org.dspace.xoai.dataprovider.parameters.OAIRequest;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.exceptions.InvalidResumptionTokenException;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.dspace.xoai.model.oaipmh.Request;
import org.dspace.xoai.services.api.DateProvider;
import org.dspace.xoai.services.api.ResumptionTokenFormat;
import org.dspace.xoai.services.impl.SimpleResumptionTokenFormat;
import org.dspace.xoai.services.impl.UTCDateProvider;

import com.lyncode.builder.Builder;

public class DataProvider {
  private static Logger log = Logger.getLogger(DataProvider.class);

  public static DataProvider dataProvider(Context context, Repository repository) {
    return new DataProvider(context, repository);
  }

  private Repository repository;
  private DateProvider dateProvider;

  private final IdentifyHandler identifyHandler;
  private final GetRecordHandler getRecordHandler;
  private final ListSetsHandler listSetsHandler;
  private final ListRecordsHandler listRecordsHandler;
  private final ListIdentifiersHandler listIdentifiersHandler;
  private final ListMetadataFormatsHandler listMetadataFormatsHandler;
  private final ErrorHandler errorsHandler;

  public DataProvider(Context context, Repository repository) {
    this.repository = repository;
    this.dateProvider = new UTCDateProvider();

    this.identifyHandler = new IdentifyHandler(context, repository);
    this.listSetsHandler = new ListSetsHandler(context, repository);
    this.listMetadataFormatsHandler = new ListMetadataFormatsHandler(context, repository);
    this.listRecordsHandler = new ListRecordsHandler(context, repository);
    this.listIdentifiersHandler = new ListIdentifiersHandler(context, repository);
    this.getRecordHandler = new GetRecordHandler(context, repository);
    this.errorsHandler = new ErrorHandler();
  }

  public OAIPMH handle(Builder<OAIRequest> builder) throws OAIException {
    return handle(builder.build());
  }

  public OAIPMH handle(OAIRequest requestParameters) throws OAIException {
    log.debug("Starting handling OAI request");
    Request request = new Request(repository.getConfiguration().getBaseUrl()).withVerbType(requestParameters.get(Verb))
      .withResumptionToken(requestParameters.get(ResumptionToken)).withIdentifier(requestParameters.get(Identifier))
      .withMetadataPrefix(requestParameters.get(MetadataPrefix)).withSet(requestParameters.get(Set))
      .withFrom(requestParameters.get(From)).withUntil(requestParameters.get(Until));

    OAIPMH response = new OAIPMH().withRequest(request).withResponseDate(dateProvider.now());
    try {
      OAICompiledRequest parameters = compileParameters(requestParameters, repository.getResumptionTokenFormatter());

      switch (request.getVerbType()) {
        case Identify:
          response.withVerb(identifyHandler.handle(parameters));
          break;
        case ListSets:
          response.withVerb(listSetsHandler.handle(parameters));
          break;
        case ListMetadataFormats:
          response.withVerb(listMetadataFormatsHandler.handle(parameters));
          break;
        case GetRecord:
          response.withVerb(getRecordHandler.handle(parameters));
          break;
        case ListIdentifiers:
          response.withVerb(listIdentifiersHandler.handle(parameters));
          break;
        case ListRecords:
          response.withVerb(listRecordsHandler.handle(parameters));
          break;
      }
    } catch (HandlerException e) {
      log.debug(e.getMessage(), e);
      response.withError(errorsHandler.handle(e));
    }

    return response;
  }

  private OAICompiledRequest compileParameters(OAIRequest requestParameters,
    ResumptionTokenFormat resumptionTokenFormat) throws IllegalVerbException, UnknownParameterException,
    BadArgumentException, DuplicateDefinitionException, BadResumptionToken {
    try {
      return requestParameters.compile(resumptionTokenFormat);
    } catch (InvalidResumptionTokenException e) {
      throw new BadResumptionToken("The resumption token is invalid");
    }
  }
}
