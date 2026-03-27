package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.TestCategory;

import java.util.*;

/**
 * Generates array and object duplication test permutations:
 * - Duplicate array elements
 * - Duplicate entire objects
 * - Nested structure duplication
 */
public class ArrayObjectDuplicationGenerator implements TestGenerator {
    private int permutationCounter = 0;

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, jsonScan.models.PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();

        // Duplicate arrays
        if (jsonElement.isJsonObject()) {
            duplicateArrays(jsonElement.getAsJsonObject(), originalJson, permutations);
            duplicateObjects(jsonElement.getAsJsonObject(), originalJson, permutations);
        } else if (jsonElement.isJsonArray()) {
            // Duplicate the entire array
            String duplicated = duplicateArrayElements(jsonElement.getAsJsonArray());
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, duplicated,
                    getCategory(), "Duplicate all array elements"));
        }

        return permutations;
    }

    private void duplicateArrays(JsonObject obj, String originalJson, List<JsonPermutation> permutations) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                JsonArray arr = entry.getValue().getAsJsonArray();

                // Duplicate array elements (2x)
                JsonElement modified = deepCopy(obj);
                JsonArray newArr = new JsonArray();
                for (JsonElement elem : arr) {
                    newArr.add(elem);
                }
                for (JsonElement elem : arr) {
                    newArr.add(deepCopy(elem));
                }
                modified.getAsJsonObject().add(entry.getKey(), newArr);

                String mutated = new Gson().toJson(modified);
                permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated,
                        getCategory(), "Duplicate array elements (key: " + entry.getKey() + ")"));

                // Duplicate array elements (3x)
                JsonElement modified3x = deepCopy(obj);
                JsonArray newArr3x = new JsonArray();
                for (int i = 0; i < 3; i++) {
                    for (JsonElement elem : arr) {
                        newArr3x.add(deepCopy(elem));
                    }
                }
                modified3x.getAsJsonObject().add(entry.getKey(), newArr3x);

                String mutated3x = new Gson().toJson(modified3x);
                permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated3x,
                        getCategory(), "Triple array elements (key: " + entry.getKey() + ")"));
            } else if (entry.getValue().isJsonObject()) {
                duplicateArrays(entry.getValue().getAsJsonObject(), originalJson, permutations);
            }
        }
    }

    private void duplicateObjects(JsonObject obj, String originalJson, List<JsonPermutation> permutations) {
        List<String> objectPaths = findAllObjectPaths(obj, "");

        for (String path : objectPaths) {
            JsonElement root = new Gson().fromJson(originalJson, JsonElement.class);
            if (duplicateObjectAtPath(root, path)) {
                String mutated = new Gson().toJson(root);
                permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated,
                        getCategory(), "Duplicate object at path: " + path));
            }
        }
    }

    private List<String> findAllObjectPaths(JsonElement element, String currentPath) {
        List<String> paths = new ArrayList<>();

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (entry.getValue().isJsonObject()) {
                    paths.add(newPath);
                    paths.addAll(findAllObjectPaths(entry.getValue(), newPath));
                } else if (entry.getValue().isJsonArray()) {
                    paths.addAll(findAllObjectPaths(entry.getValue(), newPath));
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                if (arr.get(i).isJsonObject()) {
                    paths.add(newPath);
                    paths.addAll(findAllObjectPaths(arr.get(i), newPath));
                } else if (arr.get(i).isJsonArray()) {
                    paths.addAll(findAllObjectPaths(arr.get(i), newPath));
                }
            }
        }

        return paths;
    }

    private boolean duplicateObjectAtPath(JsonElement root, String targetPath) {
        int lastDot = targetPath.lastIndexOf('.');
        int lastBracket = targetPath.lastIndexOf('[');

        if (lastDot == -1 && lastBracket == -1) {
            if (root.isJsonObject()) {
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has(targetPath)) {
                    JsonElement toDuplicate = rootObj.get(targetPath);
                    rootObj.add(targetPath + "_duplicate", deepCopy(toDuplicate));
                    return true;
                }
            }
        } else {
            String parentPath = targetPath.substring(0, Math.max(lastDot, lastBracket));
            String key = targetPath.substring(Math.max(lastDot, lastBracket) + 1);

            JsonElement parent = navigateToPath(root, parentPath);
            if (parent != null && parent.isJsonObject()) {
                JsonObject parentObj = parent.getAsJsonObject();
                if (parentObj.has(key)) {
                    JsonElement toDuplicate = parentObj.get(key);
                    parentObj.add(key + "_duplicate", deepCopy(toDuplicate));
                    return true;
                }
            }
        }

        return false;
    }

    private JsonElement navigateToPath(JsonElement root, String path) {
        if (path.isEmpty()) {
            return root;
        }

        JsonElement current = root;
        String[] parts = path.split("\\.");

        for (String part : parts) {
            if (part.contains("[")) {
                String key = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));

                if (current.isJsonObject() && current.getAsJsonObject().has(key)) {
                    JsonElement array = current.getAsJsonObject().get(key);
                    if (array.isJsonArray() && index < array.getAsJsonArray().size()) {
                        current = array.getAsJsonArray().get(index);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                if (current.isJsonObject() && current.getAsJsonObject().has(part)) {
                    current = current.getAsJsonObject().get(part);
                } else {
                    return null;
                }
            }
        }

        return current;
    }

    private String duplicateArrayElements(JsonArray arr) {
        JsonArray newArr = new JsonArray();
        for (JsonElement elem : arr) {
            newArr.add(elem);
            newArr.add(deepCopy(elem));
        }
        return new Gson().toJson(newArr);
    }

    private JsonElement deepCopy(JsonElement element) {
        return new Gson().fromJson(element.toString(), JsonElement.class);
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.ARRAY_OBJECT_DUPLICATION;
    }
}
