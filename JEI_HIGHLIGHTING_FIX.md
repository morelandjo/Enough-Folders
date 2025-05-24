# JEI Highlighting Fix Test

## Test Case: JEI Entire GUI Area Highlighting

### Problem
JEI was only highlighting all but the last row of ingredients in the folder, instead of highlighting the entire GUI area like EMI does.

### Root Cause
The issue was caused by overlapping targets in the JEI `DragDropHandler.getTargets()` method:
1. **Entire folder area target** - should highlight the full GUI
2. **Content area target** - was conflicting with the entire area target
3. **Individual folder button targets** - working correctly

### Solution
Removed the overlapping content area target to match EMI's approach:
- ✅ Keep the entire folder area target (matches EMI behavior)
- ❌ Remove the content area target (was causing conflicts)
- ✅ Keep individual folder button targets (for precise folder selection)

### Changes Made
- **File**: `DragDropHandler.java`
- **Change**: Removed content area target creation and related logic
- **Import**: Removed unused `EnoughFolders` import

### Expected Behavior After Fix
When dragging ingredients from JEI:
1. **Entire GUI area** should be highlighted (like EMI)
2. **Individual folder buttons** should also be highlighted
3. **No partial highlighting** issues (missing last row)

### Testing
To test this fix:
1. Launch Minecraft with JEI integration
2. Open a container with folder GUI
3. Drag an ingredient from JEI
4. Verify entire GUI area is highlighted consistently

### Code State
- All targets now use `FolderTargetFactory.createEntireFolderAreaTarget()` 
- No overlapping bounds that could cause rendering conflicts
- Consistent with EMI's highlighting behavior
