package com.twitter.controller.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.twitter.dto.UserDTO;
import com.twitter.model.User;
import com.twitter.persistence.UserRepository;


/**
 * @author Sanket Gore
 *
 */
@Component
public class UserRESTServiceImpl implements UserRESTService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserRESTServiceImpl.class);
	
	@Resource
	UserRepository userRepo;
	
	
	/** 
	 * @see com.twitter.controller.rest.UserRESTService#createUser(com.twitter.dto.UserDTO)
	 * The method had UserDTO as input parameter, which is a JSON representation of a user
	 * The method maps the user DTO to user entity and persists the same
	 */
	@Override
	public Response createUser(UserDTO userDTO) {
		try {
			LOGGER.info(">>createUser");
			if (userRepo.getUserNameCount(userDTO.getUserName()) == 0) {
				User user = new User();
				user.setFirstName(userDTO.getFirstName());
				user.setLastName(userDTO.getLastName());
				user.setUserName(userDTO.getUserName());
				userRepo.save(user);
				LOGGER.info("<<createUser, {} {}", userDTO.getFirstName(), userDTO.getLastName());
				return Response.status(HttpStatus.CREATED.value()).build();
			} else {
				LOGGER.info("<<createUser :: userName already exists {}", userDTO.getUserName());
				return Response.status(HttpStatus.CONFLICT.value()).build();
			}
		} catch (Exception ex) {
			LOGGER.error("<<createUser :: Failed {}", ex);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
		}
	}
	
	/** 
	 * @see com.twitter.controller.rest.UserRESTService#deleteUser(java.lang.String)
	 * The method had userId as input parameter.
	 * The method is declared transactional as it clears all the associations of a user - followers, tweets & replies
	 */
	@Override
	@Transactional
	public Response deleteUser(String userId) {
		try {
			LOGGER.info(">>deleteUser");
			userRepo.clearFollowers(Long.parseLong(userId));
			userRepo.deleteById(Long.parseLong(userId));
			LOGGER.info("<<deleteUser :: User deleted successfully with tweets, followers and replies");
			return Response.noContent().build();
		} catch(NumberFormatException nex) {
			LOGGER.info("<<deleteUser :: Failed, bad userId parameter");
			return Response.status(HttpStatus.BAD_REQUEST.value()).build();
		} catch (Exception ex) {
			LOGGER.error("<<deleteUser :: Failed {}", ex);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
		}
	}
	
	/** 
	 * @see com.twitter.controller.rest.UserRESTService#getUsers()
	 * The method fetches all user entities which are mapped to user json objects
	 */
	@Override
	public Response getUsers() {
		try {
			LOGGER.info(">>getUsers");
			List<UserDTO> userDTOs = userRepo.findAll().stream().map(entry -> new UserDTO.Builder()
											.firstName(entry.getFirstName())
											.lastName(entry.getLastName())
											.userName(entry.getUserName())
											.userId(entry.getUserId())
											.build())
											.collect(Collectors.toCollection(ArrayList::new));
			LOGGER.info("<<getUsers :: list size {}", userDTOs.size());
			return Response.ok(userDTOs).build();
		} catch (Exception ex) {
			LOGGER.error("<<getUsers :: Failed {}", ex);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
		}
	}
	
	/** 
	 * @see com.twitter.controller.rest.UserRESTService#followUser(java.lang.String, java.lang.String)
	 * The method had follower UserId and followed UserId as String input parameters.
	 * The method add a new association for a user, if doesnt exists
	 */
	@Override
	public Response followUser(String followerUserId, String followedUserId) {
		try {
			LOGGER.info(">>followUser");
			Optional<User> followerUserO = findUser(Long.parseLong(followerUserId));
			Optional<User> followedUserO = findUser(Long.parseLong(followedUserId));
			
			if(followedUserO.isPresent() && followerUserO.isPresent()) {
				
				User followedUser = followedUserO.get();
				User followerUser = followerUserO.get();
				
				if(!(followedUser.getUserId().equals(followerUser.getUserId()))) {
					followedUser.getFollowerUser().add(followerUser);
					userRepo.save(followedUser);
					LOGGER.info("<<followUser :: User {} followed {}", followerUser.getUserName(), followedUser.getUserName());
					return Response.status(HttpStatus.OK.value()).build();
				}
				LOGGER.info("<<followUser same user is forbidden");
				return Response.status(HttpStatus.FORBIDDEN.value()).build();
			} 
			LOGGER.info("<<followUser is forbidden");
			return Response.status(HttpStatus.NOT_FOUND.value()).build();
		} catch(NumberFormatException nex) {
			LOGGER.info("<<followUser :: Failed, bad userId parameter");
			return Response.status(HttpStatus.BAD_REQUEST.value()).build();
		} catch (Exception ex) {
			LOGGER.info("<<followUser :: Failed {}", ex);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
		}
	}

	/** 
	 * @see com.twitter.controller.rest.UserRESTService#unfollowUser(java.lang.String, java.lang.String)
	 * The method had follower UserId and followed UserId as String input parameters.
	 * The method removes association for a user, if exists
	 */
	@Override
	public Response unfollowUser(String followerUserId, String followedUserId) {
		try {
			LOGGER.info(">>unfollowUser");
			Optional<User> followerUser = findUser(Long.parseLong(followerUserId));
			Optional<User> followedUser = findUser(Long.parseLong(followedUserId));

			if (followedUser.isPresent() && followerUser.isPresent()) {
				Long followerId = followerUser.get().getUserId();
				int followerSize = followedUser.get().getFollowerUser().size();
				followedUser.get().getFollowerUser().removeIf(entry -> entry.getUserId().equals(followerId));
				if (followerSize != followedUser.get().getFollowerUser().size()) {
					userRepo.save(followedUser.get());
					LOGGER.info("<<unfollowUser successful");
					return Response.status(HttpStatus.OK.value()).build();
				}
				LOGGER.info("<<unfollowUser :: forbidden");
				return Response.status(HttpStatus.FORBIDDEN.value()).build();
			}
			LOGGER.info("<<unfollowUser :: followed/follower user not found");
			return Response.status(HttpStatus.NOT_FOUND.value()).build();
		} catch(NumberFormatException nex) {
			LOGGER.info("<<unfollowUser :: Failed, bad userId parameter");
			return Response.status(HttpStatus.BAD_REQUEST.value()).build();
		} catch (Exception ex) {
			LOGGER.info("<<unfollowUser :: Failed {}", ex);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
		}
	}

	/** 
	 * @see com.twitter.controller.rest.UserRESTService#getfollowers(java.lang.String)
	 * The method fetches all users following the input UserId
	 */
	@Override
	public Response getfollowers(String userId) {
		try {
			LOGGER.info(">>getfollowers :: userId {}", userId);
			Optional<User> user = findUser(Long.parseLong(userId));
			List<UserDTO> result = new ArrayList<>();
			if (user.isPresent()) {
				result = user.get().getFollowerUser().stream().map(entry -> new UserDTO.Builder()
										.firstName(entry.getFirstName())
										.lastName(entry.getLastName())
										.userName(entry.getUserName())
										.userId(entry.getUserId())
										.build())
										.collect(Collectors.toCollection(ArrayList::new));
			}
			LOGGER.info("<<getfollowers :: follower list size {} ", result.size());
			if (!result.isEmpty()) {
				return Response.ok(result).build();
			} else {
				return Response.noContent().build();
			}
		} catch(NumberFormatException nex) {
			LOGGER.info("<<getfollowers :: Failed, bad userId parameter");
			return Response.status(HttpStatus.BAD_REQUEST.value()).build();
		} catch (Exception ex) {
			LOGGER.info("<<getfollowers :: Failed {}", ex);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
		}
	}

	/**
	 * @see com.twitter.controller.rest.UserRESTService#findUser(java.lang.Long)
	 * The method fetches user by Id
	 */
	public Optional<User> findUser(Long id) {
		return userRepo.findById(id);
	}

}
