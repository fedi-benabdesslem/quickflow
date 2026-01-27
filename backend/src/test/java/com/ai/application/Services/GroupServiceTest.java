package com.ai.application.Services;

import com.ai.application.Repositories.ContactRepository;
import com.ai.application.Repositories.GroupRepository;
import com.ai.application.model.Entity.Contact;
import com.ai.application.model.Entity.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupService.
 * 
 * Tests cover:
 * - Group CRUD operations
 * - Member management (add/remove)
 * - Duplicate name validation
 * - Group search
 * 
 * Note: Repository is mocked to isolate GroupService logic.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private ContactRepository contactRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService();
        // Use reflection to inject mocks since GroupService uses @Autowired
        try {
            java.lang.reflect.Field groupRepoField = GroupService.class.getDeclaredField("groupRepository");
            groupRepoField.setAccessible(true);
            groupRepoField.set(groupService, groupRepository);
            
            java.lang.reflect.Field contactRepoField = GroupService.class.getDeclaredField("contactRepository");
            contactRepoField.setAccessible(true);
            contactRepoField.set(groupService, contactRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }

    @Nested
    @DisplayName("createGroup()")
    class CreateGroupTests {

        @Test
        @DisplayName("Should create group with name and description")
        void createGroupWithNameAndDescription() {
            String userId = "user-123";
            String name = "Engineering Team";
            String description = "All engineering members";
            
            when(groupRepository.findByUserIdAndName(userId, name)).thenReturn(Optional.empty());
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> {
                Group g = i.getArgument(0);
                g.setId("group-id-123");
                return g;
            });

            Group result = groupService.createGroup(userId, name, description, null);

            assertNotNull(result);
            assertEquals(name, result.getName());
            assertEquals(description, result.getDescription());
            assertEquals(userId, result.getUserId());
            verify(groupRepository).save(any(Group.class));
        }

        @Test
        @DisplayName("Should create group with initial members")
        void createGroupWithInitialMembers() {
            String userId = "user-123";
            List<String> memberIds = Arrays.asList("contact-1", "contact-2", "contact-3");
            
            when(groupRepository.findByUserIdAndName(userId, "Team")).thenReturn(Optional.empty());
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            Group result = groupService.createGroup(userId, "Team", null, memberIds);

            assertNotNull(result);
            assertEquals(3, result.getMemberIds().size());
            assertTrue(result.getMemberIds().containsAll(memberIds));
        }

        @Test
        @DisplayName("Should throw exception for duplicate group name")
        void throwExceptionForDuplicateName() {
            String userId = "user-123";
            String name = "Existing Group";
            
            Group existing = new Group(userId, name);
            when(groupRepository.findByUserIdAndName(userId, name)).thenReturn(Optional.of(existing));

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.createGroup(userId, name, null, null));

            assertEquals("A group with this name already exists", exception.getMessage());
            verify(groupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getGroup()")
    class GetGroupTests {

        @Test
        @DisplayName("Should return group with member details")
        void returnGroupWithMemberDetails() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            group.setMemberIds(Arrays.asList("contact-1", "contact-2"));
            
            Contact contact1 = new Contact("user-123", "Alice", "alice@example.com", "google");
            contact1.setId("contact-1");
            
            Contact contact2 = new Contact("user-123", "Bob", "bob@example.com", "google");
            contact2.setId("contact-2");
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(contactRepository.findById("contact-1")).thenReturn(Optional.of(contact1));
            when(contactRepository.findById("contact-2")).thenReturn(Optional.of(contact2));

            Map<String, Object> result = groupService.getGroup(groupId);

            assertNotNull(result);
            assertEquals("Test Group", result.get("name"));
            assertEquals(2, result.get("memberCount"));
            
            @SuppressWarnings("unchecked")
            List<Contact> members = (List<Contact>) result.get("members");
            assertEquals(2, members.size());
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void throwExceptionWhenGroupNotFound() {
            when(groupRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                () -> groupService.getGroup("nonexistent"));
        }

        @Test
        @DisplayName("Should exclude deleted contacts from members")
        void excludeDeletedContactsFromMembers() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            group.setMemberIds(Arrays.asList("contact-1", "contact-2"));
            
            Contact activeContact = new Contact("user-123", "Alice", "alice@example.com", "google");
            activeContact.setId("contact-1");
            
            Contact deletedContact = new Contact("user-123", "Bob", "bob@example.com", "google");
            deletedContact.setId("contact-2");
            deletedContact.setDeleted(true);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(contactRepository.findById("contact-1")).thenReturn(Optional.of(activeContact));
            when(contactRepository.findById("contact-2")).thenReturn(Optional.of(deletedContact));

            Map<String, Object> result = groupService.getGroup(groupId);

            @SuppressWarnings("unchecked")
            List<Contact> members = (List<Contact>) result.get("members");
            assertEquals(1, members.size());
            assertEquals("Alice", members.get(0).getName());
        }
    }

    @Nested
    @DisplayName("updateGroup()")
    class UpdateGroupTests {

        @Test
        @DisplayName("Should update group name")
        void updateGroupName() {
            String groupId = "group-123";
            String userId = "user-123";
            Group group = new Group(userId, "Old Name");
            group.setId(groupId);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.findByUserIdAndName(userId, "New Name")).thenReturn(Optional.empty());
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            Group result = groupService.updateGroup(groupId, "New Name", null, null);

            assertEquals("New Name", result.getName());
        }

        @Test
        @DisplayName("Should update group description")
        void updateGroupDescription() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            Group result = groupService.updateGroup(groupId, null, "New Description", null);

            assertEquals("New Description", result.getDescription());
        }

        @Test
        @DisplayName("Should update group members")
        void updateGroupMembers() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            group.setMemberIds(new ArrayList<>(Arrays.asList("old-1", "old-2")));
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            List<String> newMembers = Arrays.asList("new-1", "new-2", "new-3");
            Group result = groupService.updateGroup(groupId, null, null, newMembers);

            assertEquals(3, result.getMemberIds().size());
            assertTrue(result.getMemberIds().containsAll(newMembers));
        }

        @Test
        @DisplayName("Should throw exception for duplicate name on update")
        void throwExceptionForDuplicateNameOnUpdate() {
            String groupId = "group-123";
            String userId = "user-123";
            Group group = new Group(userId, "Group A");
            group.setId(groupId);
            
            Group otherGroup = new Group(userId, "Group B");
            otherGroup.setId("other-group-id");
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.findByUserIdAndName(userId, "Group B")).thenReturn(Optional.of(otherGroup));

            assertThrows(IllegalArgumentException.class,
                () -> groupService.updateGroup(groupId, "Group B", null, null));
        }

        @Test
        @DisplayName("Should allow same name for same group")
        void allowSameNameForSameGroup() {
            String groupId = "group-123";
            String userId = "user-123";
            Group group = new Group(userId, "Same Name");
            group.setId(groupId);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            // When name matches current name, no duplicate check should occur
            Group result = groupService.updateGroup(groupId, "Same Name", "New desc", null);

            assertEquals("Same Name", result.getName());
            assertEquals("New desc", result.getDescription());
            verify(groupRepository, never()).findByUserIdAndName(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("deleteGroup()")
    class DeleteGroupTests {

        @Test
        @DisplayName("Should delete existing group")
        void deleteExistingGroup() {
            String groupId = "group-123";
            Group group = new Group("user-123", "To Delete");
            group.setId(groupId);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

            groupService.deleteGroup(groupId);

            verify(groupRepository).delete(group);
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void throwExceptionWhenNotFound() {
            when(groupRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                () -> groupService.deleteGroup("nonexistent"));
        }
    }

    @Nested
    @DisplayName("addMembers()")
    class AddMembersTests {

        @Test
        @DisplayName("Should add new members to group")
        void addNewMembersToGroup() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            group.setMemberIds(new ArrayList<>(Arrays.asList("existing-1")));
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            Group result = groupService.addMembers(groupId, Arrays.asList("new-1", "new-2"));

            assertEquals(3, result.getMemberIds().size());
            assertTrue(result.getMemberIds().contains("existing-1"));
            assertTrue(result.getMemberIds().contains("new-1"));
            assertTrue(result.getMemberIds().contains("new-2"));
        }

        @Test
        @DisplayName("Should not duplicate existing members")
        void notDuplicateExistingMembers() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            group.setMemberIds(new ArrayList<>(Arrays.asList("member-1")));
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            Group result = groupService.addMembers(groupId, Arrays.asList("member-1", "member-2"));

            assertEquals(2, result.getMemberIds().size());
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member from group")
        void removeMemberFromGroup() {
            String groupId = "group-123";
            Group group = new Group("user-123", "Test Group");
            group.setId(groupId);
            group.setMemberIds(new ArrayList<>(Arrays.asList("member-1", "member-2", "member-3")));
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));

            Group result = groupService.removeMember(groupId, "member-2");

            assertEquals(2, result.getMemberIds().size());
            assertFalse(result.getMemberIds().contains("member-2"));
        }
    }

    @Nested
    @DisplayName("getGroupsForContact()")
    class GetGroupsForContactTests {

        @Test
        @DisplayName("Should return groups containing contact")
        void returnGroupsContainingContact() {
            String userId = "user-123";
            String contactId = "contact-1";
            
            Group group1 = new Group(userId, "Group 1");
            Group group2 = new Group(userId, "Group 2");
            
            when(groupRepository.findByUserIdAndMemberIdsContaining(userId, contactId))
                .thenReturn(Arrays.asList(group1, group2));

            List<Group> result = groupService.getGroupsForContact(userId, contactId);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("getGroupCount()")
    class GetGroupCountTests {

        @Test
        @DisplayName("Should return count of groups for user")
        void returnGroupCountForUser() {
            String userId = "user-123";
            when(groupRepository.countByUserId(userId)).thenReturn(5L);

            long count = groupService.getGroupCount(userId);

            assertEquals(5L, count);
        }
    }

    @Nested
    @DisplayName("searchGroups()")
    class SearchGroupsTests {

        @Test
        @DisplayName("Should search groups by name case-insensitively")
        void searchGroupsByNameCaseInsensitive() {
            String userId = "user-123";
            
            Group group1 = new Group(userId, "Engineering Team");
            group1.setId("g1");
            group1.setMemberIds(new ArrayList<>());
            
            Group group2 = new Group(userId, "Marketing");
            group2.setId("g2");
            group2.setMemberIds(new ArrayList<>());
            
            Group group3 = new Group(userId, "Engineering Interns");
            group3.setId("g3");
            group3.setMemberIds(new ArrayList<>());
            
            when(groupRepository.findByUserId(userId))
                .thenReturn(Arrays.asList(group1, group2, group3));

            List<Map<String, Object>> result = groupService.searchGroups(userId, "engineer", 10);

            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(g -> "Engineering Team".equals(g.get("name"))));
            assertTrue(result.stream().anyMatch(g -> "Engineering Interns".equals(g.get("name"))));
        }

        @Test
        @DisplayName("Should limit search results")
        void limitSearchResults() {
            String userId = "user-123";
            
            List<Group> groups = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Group g = new Group(userId, "Team " + i);
                g.setId("g" + i);
                g.setMemberIds(new ArrayList<>());
                groups.add(g);
            }
            
            when(groupRepository.findByUserId(userId)).thenReturn(groups);

            List<Map<String, Object>> result = groupService.searchGroups(userId, "Team", 3);

            assertEquals(3, result.size());
        }
    }
}
