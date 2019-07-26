/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.dataprovider.handlers.helpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dspace.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.filter.Scope;
import org.dspace.xoai.dataprovider.filter.ScopedFilter;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.handlers.results.ListItemsResults;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.dspace.xoai.dataprovider.repository.ItemRepository;

public class ItemRepositoryHelper {
  private ItemRepository itemRepository;

  public ItemRepositoryHelper(ItemRepository itemRepository) {
    super();
    this.itemRepository = itemRepository;
  }

  public ListItemIdentifiersResult getItemIdentifiers(Context context, int offset, int length, String metadataPrefix,
    String scrollId) throws CannotDisseminateFormatException, OAIException {
    return itemRepository.getItemIdentifiers(getScopedFilters(context, metadataPrefix), offset, length, scrollId);
  }

  public ListItemIdentifiersResult getItemIdentifiers(Context context, int offset, int length, String metadataPrefix,
    Date from, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    return itemRepository.getItemIdentifiers(filters, offset, length, from, scrollId);
  }

  private List<ScopedFilter> getScopedFilters(Context context, String metadataPrefix)
    throws CannotDisseminateFormatException {
    List<ScopedFilter> filters = new ArrayList<ScopedFilter>();
    if (context.hasCondition())
      filters.add(new ScopedFilter(context.getCondition(), Scope.Context));

    MetadataFormat metadataFormat = context.formatForPrefix(metadataPrefix);
    if (metadataFormat.hasCondition())
      filters.add(new ScopedFilter(metadataFormat.getCondition(), Scope.MetadataFormat));
    return filters;
  }

  public ListItemIdentifiersResult getItemIdentifiersUntil(Context context, int offset, int length,
    String metadataPrefix, Date until, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    return itemRepository.getItemIdentifiersUntil(filters, offset, length, until, scrollId);
  }

  public ListItemIdentifiersResult getItemIdentifiers(Context context, int offset, int length, String metadataPrefix,
    Date from, Date until, String scrollId) throws CannotDisseminateFormatException, OAIException {
    return itemRepository.getItemIdentifiers(getScopedFilters(context, metadataPrefix), offset, length, from, until,
      scrollId);
  }

  public ListItemIdentifiersResult getItemIdentifiers(Context context, int offset, int length, String metadataPrefix,
    String setSpec, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItemIdentifiers(filters, offset, length, scrollId);
    } else
      return itemRepository.getItemIdentifiers(filters, offset, length, setSpec, scrollId);
  }

  public ListItemIdentifiersResult getItemIdentifiers(Context context, int offset, int length, String metadataPrefix,
    String setSpec, Date from, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItemIdentifiers(filters, offset, length, from, scrollId);
    } else
      return itemRepository.getItemIdentifiers(filters, offset, length, setSpec, from, scrollId);
  }

  public ListItemIdentifiersResult getItemIdentifiersUntil(Context context, int offset, int length,
    String metadataPrefix, String setSpec, Date until, String scrollId)
    throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItemIdentifiersUntil(filters, offset, length, until, scrollId);
    } else
      return itemRepository.getItemIdentifiersUntil(filters, offset, length, setSpec, until, scrollId);
  }

  public ListItemIdentifiersResult getItemIdentifiers(Context context, int offset, int length, String metadataPrefix,
    String setSpec, Date from, Date until, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItemIdentifiers(filters, offset, length, from, until, scrollId);
    } else
      return itemRepository.getItemIdentifiers(filters, offset, length, setSpec, from, until, scrollId);
  }

  public ListItemsResults getItems(Context context, int offset, int length, String metadataPrefix, String scrollId)
    throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    return itemRepository.getItems(filters, offset, length, scrollId);
  }

  public ListItemsResults getItems(Context context, int offset, int length, String metadataPrefix, Date from,
    String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    return itemRepository.getItems(filters, offset, length, from, scrollId);
  }

  public ListItemsResults getItemsUntil(Context context, int offset, int length, String metadataPrefix, Date until,
    String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    return itemRepository.getItemsUntil(filters, offset, length, until, scrollId);
  }

  public ListItemsResults getItems(Context context, int offset, int length, String metadataPrefix, Date from,
    Date until, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    return itemRepository.getItems(filters, offset, length, from, until, scrollId);
  }

  public ListItemsResults getItems(Context context, int offset, int length, String metadataPrefix, String setSpec,
    String scrollId) throws CannotDisseminateFormatException, OAIException {

    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItems(filters, offset, length, scrollId);
    } else
      return itemRepository.getItems(filters, offset, length, setSpec, scrollId);
  }

  public ListItemsResults getItems(Context context, int offset, int length, String metadataPrefix, String setSpec,
    Date from, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItems(filters, offset, length, from, scrollId);
    } else
      return itemRepository.getItems(filters, offset, length, setSpec, from, scrollId);
  }

  public ListItemsResults getItemsUntil(Context context, int offset, int length, String metadataPrefix, String setSpec,
    Date until, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItemsUntil(filters, offset, length, until, scrollId);
    } else
      return itemRepository.getItemsUntil(filters, offset, length, setSpec, until, scrollId);
  }

  public ListItemsResults getItems(Context context, int offset, int length, String metadataPrefix, String setSpec,
    Date from, Date until, String scrollId) throws CannotDisseminateFormatException, OAIException {
    List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
    if (context.isStaticSet(setSpec)) {
      filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
      return itemRepository.getItems(filters, offset, length, from, until, scrollId);
    } else
      return itemRepository.getItems(filters, offset, length, setSpec, from, until, scrollId);
  }

  public Item getItem(String identifier) throws IdDoesNotExistException, OAIException {
    return itemRepository.getItem(identifier);
  }
}
