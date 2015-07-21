package nc.ui.wlm.m21.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nc.ui.bd.ref.UFRefManage;
import nc.ui.pu.m21.view.OrderBillForm;
import nc.ui.pu.uif2.PUBillManageModel;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pubapp.uif2app.query2.model.ModelDataManager;
import nc.ui.uif2.NCAction;
import nc.ui.wlm.ext.WLMExtUtil;
import nc.vo.pu.m21.entity.OrderItemVO;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pubapp.AppContext;
import nc.vo.trade.pub.IBillStatus;
import nc.vo.wlm.x0k301.AggTeamWorkVO;
import nc.vo.wlm.x0k301.TeamWorkBVO;
import nc.vo.wlm.x0k301.TeamWorkHVO;

public class ProcureSynergyAction extends NCAction {
	private final String BTNNAME = "协同";
	private final String ACTIONCODE = "ProcureSynergyAction";
	private ModelDataManager dataManager;
	private PUBillManageModel model;
	private OrderBillForm BillForm;
	private UFRefManage m_refManage;
	private WLMExtUtil WLMExtUtils;
	/** 内部协同 */
	public static final String COORPBILL = "X005";
	public PUBillManageModel getModel() {
		return model;
	}

	public void setModel(PUBillManageModel model) {
		this.model = model;
	}
	
	public ModelDataManager getDataManager() {
		return dataManager;
	}

	public void setDataManager(ModelDataManager dataManager) {
		this.dataManager = dataManager;
	}
	
	public OrderBillForm getBillForm() {
		return BillForm;
	}

	public void setBillForm(OrderBillForm billForm) {
		BillForm = billForm;
	}

	public ProcureSynergyAction() {
		this.setCode(ACTIONCODE);
		this.setBtnName(BTNNAME);
		this.putValue(ACTIONCODE, ACTIONCODE);
	}
	
	public nc.ui.bd.ref.UFRefManage getRef() throws BusinessException {
			m_refManage = getWLMExtUtil().getRef(BillForm);

		return m_refManage;
	}
	
	public WLMExtUtil getWLMExtUtil() throws BusinessException {
		if (this.WLMExtUtils == null) {
			this.WLMExtUtils = new WLMExtUtil();
		}
		return this.WLMExtUtils;	
	}
	
	@Override
	public void doAction(ActionEvent arg0) throws Exception {
		List<OrderVO> povos = getOrderVO();
		for (int i = 0; i < povos.size(); i++) {
			if(!povos.get(i).getParentVO().getAttributeValue("forderstatus").toString().equals("3")){
				MessageDialog.showErrorDlg(getBillForm(), "内部协同", "所选数据的第"+(i+1)+"条没有审批通过，无法协同!");
				return;
			}				
		}
		if (povos == null || povos.size() <= 0) {
			MessageDialog.showHintDlg(getBillForm(), "内部协同", "请选择待协同的单据!");
			return;
		}
		
		int ret = getRef().showModal();  
		if (ret == this.m_refManage.ID_OK) {
			String corpname = m_refManage.getRefModel().getRefNameValue();
			String pk_corp = m_refManage.getRefModel().getPkValue();
			for (int i = 0; i < povos.size(); i++) {
				String pk_org =povos.get(i).getParentVO().getAttributeValue("pk_org").toString();
				String code =povos.get(i).getParentVO().getAttributeValue("vbillcode").toString();
				if (pk_corp.equals(pk_org)) {
					MessageDialog.showErrorDlg(getBillForm(), "内部协同", "单据"+code+"不能协同到同公司!");
					return;
				}
				for (int j = 0; j < povos.get(i).getChildrenVO().length; j++) {
					if(povos.get(i).getChildrenVO()[j].getAttributeValue("pk_recvstordoc")!=null){
						getWLMExtUtil().Cooperativequery(povos.get(i).getChildrenVO()[j].getAttributeValue("pk_recvstordoc").toString(),povos.get(i).getParentVO().getAttributeValue("vbillcode").toString(),povos.get(i).getChildrenVO()[j].getAttributeValue("pk_arrvstoorg").toString());
					}else{
						MessageDialog.showErrorDlg(getBillForm(), "内部协同", "单据"+povos.get(i).getParentVO().getAttributeValue("vbillcode").toString()+"收货仓库不能为空!");
						return;
					}
				}			
			}
			if (MessageDialog.showYesNoDlg(getBillForm(), "内部协同", "是否将采购订单单协同到公司：【" + corpname + "】?") == MessageDialog.ID_YES){
				if (povos != null && povos.size() > 0) {
					List<String> billids = new ArrayList<String>();
					for (OrderVO vo : povos) {
						billids.add(vo.getParentVO().getAttributeValue("vbillcode").toString());
					}
					Map<String,UFBoolean> hasAft =getWLMExtUtil().hasCooped(billids);
					StringBuffer msg = new StringBuffer("");
					boolean canCorp = true;
					for (OrderVO vo : povos) {
						UFBoolean hasCoorp = hasAft.get(vo.getParentVO().getAttributeValue("vbillcode").toString());
						if (hasCoorp != null && hasCoorp.booleanValue()) {
							msg.append("【" + vo.getParentVO().getAttributeValue("vbillcode").toString() + "】已经协同，\r\n");
							canCorp = false;
							continue;
						}
					}
						if (!canCorp) {
							msg.append("已进行过协同操作，不能再协同操作!");
							MessageDialog.showErrorDlg(getBillForm(), "内部协同", msg.toString());
							return;
						}
						UFDate copdate = AppContext.getInstance().getBusiDate();
						genCoorpBillByPO(povos.toArray(new OrderVO[0]), copdate, pk_corp);
						MessageDialog.showHintDlg(getBillForm(), "内部协同", "协同成功!");
					
				}else {
					MessageDialog.showHintDlg(getBillForm(), "内部协同", "请选择待协同的单据!");
				}
				
			}
			
		}
	}
	
	public List genCoorpBillByPO(OrderVO[] poVOs, UFDate date, String coorp) throws BusinessException {
		List ret = new ArrayList();
		if (poVOs != null || poVOs.length > 0) {
			AggTeamWorkVO[] bills = new AggTeamWorkVO[poVOs.length];
			for (int i = 0; i < poVOs.length; i++) {
				bills[i] = chgPO2WLM3(poVOs[i], date, coorp);
				getWLMExtUtil().getBillAction().processAction("SAVEBASE", "X005",null, bills[i], null, null);
			}
	
		}
		return ret;
	}
	
	private List<OrderVO> getOrderVO() throws BusinessException {
		Object[] objs =(Object[])getModel().getSelectedOperaDatas();
		List<OrderVO> OrderVO = new ArrayList<OrderVO>();
		if (objs!=null) {
			for (Object object : objs) {
				OrderVO.add((OrderVO)object);
			}		
		}
		return OrderVO;
	}
	private AggTeamWorkVO chgPO2WLM3 (OrderVO poVO, UFDate date, String coorp) throws BusinessException {
		AggTeamWorkVO billVO = null;
		if (poVO != null) {	
			billVO = new AggTeamWorkVO();
			TeamWorkHVO hVO = new TeamWorkHVO();	
			hVO.setStatus(VOStatus.NEW);
			hVO.setDbilldate(new UFDate());
			hVO.setDmakedate(new UFDate());
			hVO.setPk_org(coorp);
			hVO.setPk_group(AppContext.getInstance().getPkGroup());
			hVO.setVbillmaker(AppContext.getInstance().getPkUser());
			hVO.setMaketime(AppContext.getInstance().getServerTime());
			hVO.setPk_oporg(poVO.getParentVO().getAttributeValue("pk_org").toString());	// 对应的协同公司
			hVO.setIoptype(hVO.OP_IN);  //协作出入库类型
			hVO.setDplanopdate(date);	// 预计协作日期
			hVO.setFstatusflag(IBillStatus.FREE);
			OrderItemVO[] pobvos = (OrderItemVO[]) poVO.getChildrenVO();
			TeamWorkBVO[] bvos = new TeamWorkBVO[pobvos.length];
			Set<String> invbasids = new HashSet<String>();			
			for (int i = 0; i < pobvos.length; i++ ) {				
				invbasids.add(pobvos[i].getPk_material());
				bvos[i] = new TeamWorkBVO();
				bvos[i].setStatus(VOStatus.NEW);
				bvos[i].setPk_material(pobvos[i].getPk_material());
				bvos[i].setVfree1(pobvos[i].getVfree1());
				bvos[i].setVfree2(pobvos[i].getVfree2());
				bvos[i].setVfree3(pobvos[i].getVfree3());
				bvos[i].setVfree4(pobvos[i].getVfree4());
				bvos[i].setVfree5(pobvos[i].getVfree5());
				bvos[i].setInvbatchcode(pobvos[i].getVbatchcode());
				bvos[i].setNnum(pobvos[i].getNnum());
				bvos[i].setVrowno(pobvos[i].getCrowno());
				bvos[i].setPk_storedoc(pobvos[i].getPk_recvstordoc());
				bvos[i].setVsrccode(poVO.getParentVO().getAttributeValue("vbillcode").toString());
				bvos[i].setVsrcid(poVO.getParentVO().getAttributeValue("pk_order").toString());	
				bvos[i].setVsrcbid(pobvos[i].getPk_order_b());	
			}
			getWLMExtUtil().convertInvmanid(invbasids, coorp, bvos);
			billVO.setParentVO(hVO);
			billVO.setChildrenVO(bvos);
		}
		return billVO;
	}
	
}
