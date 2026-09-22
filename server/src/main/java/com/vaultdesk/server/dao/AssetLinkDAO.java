package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AssetLinkDAO {
    private final JdbcTemplate jdbc;

    public AssetLinkDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns linked assets in either direction, with basic display fields + the link row's own id (for unlinking). */
    public List<Map<String, Object>> getLinksForAsset(int assetId) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(jdbc.queryForList(
                "SELECT l.id as link_id, l.link_type, a.id, a.asset_tag, a.name, a.category, a.status " +
                        "FROM asset_links l JOIN assets a ON a.id = l.linked_asset_id " +
                        "WHERE l.asset_id = ?", assetId));
        result.addAll(jdbc.queryForList(
                "SELECT l.id as link_id, l.link_type, a.id, a.asset_tag, a.name, a.category, a.status " +
                        "FROM asset_links l JOIN assets a ON a.id = l.asset_id " +
                        "WHERE l.linked_asset_id = ?", assetId));
        return result;
    }

    /** Returns 0 if already linked or self-link attempted; otherwise the new link's row id. */
    public int addLink(int assetId, int linkedAssetId, String linkType) {
        if (assetId == linkedAssetId) return 0;
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset_links WHERE " +
                        "(asset_id = ? AND linked_asset_id = ?) OR (asset_id = ? AND linked_asset_id = ?)",
                Integer.class, assetId, linkedAssetId, linkedAssetId, assetId);
        if (existing != null && existing > 0) return 0;

        jdbc.update(
                "INSERT INTO asset_links (asset_id, linked_asset_id, link_type) VALUES (?, ?, ?)",
                assetId, linkedAssetId, linkType);
        Integer newId = jdbc.queryForObject("SELECT MAX(id) FROM asset_links", Integer.class);
        return newId != null ? newId : 0;
    }

    public int removeLink(int linkId) {
        return jdbc.update("DELETE FROM asset_links WHERE id = ?", linkId);
    }

    /** For an employee's assigned assets, attach each asset's linked accessories. Used by item 13/14. */
    public List<Map<String, Object>> getLinksForAssetIds(List<Integer> assetIds) {
        if (assetIds.isEmpty()) return new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < assetIds.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
        List<Object> params = new ArrayList<>(assetIds);
        List<Map<String, Object>> result = jdbc.queryForList(
                "SELECT l.asset_id as owner_asset_id, a.id, a.asset_tag, a.name, a.category, a.status " +
                        "FROM asset_links l JOIN assets a ON a.id = l.linked_asset_id " +
                        "WHERE l.asset_id IN (" + placeholders + ")", params.toArray());
        return result;
    }
}