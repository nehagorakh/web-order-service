package in.lifcare.order.api;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.response.model.Response;
import in.lifcare.order.service.OrderItemService;

@RestController
@RequestMapping(value = "/order/items")
public class OrderItemController {
	
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/sku/{sku-id}/salt-mapping", method = RequestMethod.PATCH)
	public @ResponseBody Response<Boolean> updateStatus(@PathVariable("sku-id") String sku ,@RequestBody Map<String, Object> saltMappingMap) throws Exception {

		if (saltMappingMap == null || StringUtils.isBlank(sku)) {
			throw new BadRequestException("Invalid request params.");
		}
		return new Response<Boolean>(orderItemService.updateSaltMapping(sku, saltMappingMap));
	}
	
	@Autowired
	private OrderItemService orderItemService;
	
}
